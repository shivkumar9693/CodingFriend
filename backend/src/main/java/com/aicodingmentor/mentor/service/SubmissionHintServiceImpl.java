package com.aicodingmentor.mentor.service;

import com.aicodingmentor.common.error.ApiException;
import com.aicodingmentor.common.error.ResourceNotFoundException;
import com.aicodingmentor.learning.service.LearnerMistakeTrackingService;
import com.aicodingmentor.mentor.domain.SubmissionHint;
import com.aicodingmentor.mentor.dto.AiPromptContext;
import com.aicodingmentor.mentor.dto.ExecutionMetricsContext;
import com.aicodingmentor.mentor.dto.SubmissionHintRequest;
import com.aicodingmentor.mentor.dto.SubmissionHintResponse;
import com.aicodingmentor.mentor.model.HintLevel;
import com.aicodingmentor.mentor.repository.SubmissionHintRepository;
import com.aicodingmentor.mistake.model.MistakeType;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.aicodingmentor.submission.domain.Submission;
import com.aicodingmentor.submission.repository.SubmissionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SubmissionHintServiceImpl implements SubmissionHintService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionHintServiceImpl.class);
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[a-zA-Z]*\\n[\\s\\S]*?```");

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionHintRepository hintRepository;
    private final MentorModelClient mentorModelClient;
    private final PromptBuilder promptBuilder;
    private final PromptSanitizer promptSanitizer;
    private final LearnerMistakeTrackingService mistakeTrackingService;
    private final ObjectMapper objectMapper;

    public SubmissionHintServiceImpl(
            SubmissionRepository submissionRepository,
            ProblemRepository problemRepository,
            SubmissionHintRepository hintRepository,
            MentorModelClient mentorModelClient,
            PromptBuilder promptBuilder,
            PromptSanitizer promptSanitizer,
            LearnerMistakeTrackingService mistakeTrackingService,
            ObjectMapper objectMapper
    ) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
        this.hintRepository = hintRepository;
        this.mentorModelClient = mentorModelClient;
        this.promptBuilder = promptBuilder;
        this.promptSanitizer = promptSanitizer;
        this.mistakeTrackingService = mistakeTrackingService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public SubmissionHintResponse generateSubmissionHint(UUID userId, UUID submissionId, SubmissionHintRequest request) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", submissionId));

        if (!submission.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only request hints for your own submissions");
        }

        Problem problem = problemRepository.findById(submission.getProblemId())
                .orElseThrow(() -> new ResourceNotFoundException("Problem", submission.getProblemId()));

        int requestedLevel = request.level();

        // Check already generated hint for this level to avoid redundant LLM calls
        Optional<SubmissionHint> existingHint = hintRepository.findFirstBySubmissionIdAndHintLevelOrderByCreatedAtDesc(submissionId, requestedLevel);
        if (existingHint.isPresent()) {
            SubmissionHint h = existingHint.get();
            return new SubmissionHintResponse(
                    h.getMistakeType(),
                    h.getHintLevel(),
                    h.getHint(),
                    h.getConcept(),
                    h.isRevealsSolution()
            );
        }

        // Progression guard: Prevent requesting level 4 or 5 before requesting earlier exploratory hints (levels 1-3)
        List<SubmissionHint> previousHints = hintRepository.findBySubmissionIdOrderByHintLevelAsc(submissionId);
        if (requestedLevel >= 4 && previousHints.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Progression constraint: Please explore foundational hint tiers (Levels 1-3) before requesting detailed explanations (Level 4) or solution guidance (Level 5).");
        }

        // Derive mistake type from submission verdict / details
        MistakeType mistakeType = deriveMistakeType(submission);

        // Record mistake in Day 17 tracking service
        if (mistakeType != MistakeType.NONE) {
            mistakeTrackingService.recordMistake(userId, mistakeType, problem.getTags());
        }

        // Build context
        List<String> previousHintTexts = previousHints.stream()
                .map(h -> "Level " + h.getHintLevel() + ": " + h.getHint())
                .toList();

        AiPromptContext context = AiPromptContext.builder()
                .problem(problem)
                .sourceCode(submission.getSourceCode())
                .programmingLanguage(submission.getLanguage())
                .hintLevel(mapToHintLevel(requestedLevel))
                .mistakeType(mistakeType.name())
                .compilerOrRuntimeError(submission.getErrorMessage())
                .executionMetrics(ExecutionMetricsContext.of(
                        submission.getExecutionTimeMs(),
                        null,
                        submission.getVerdict() != null && submission.getVerdict() == com.aicodingmentor.execution.model.EvaluationVerdict.ACCEPTED ? 0 : 1
                ))
                .previousHints(previousHintTexts)
                .build();

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildContextualPrompt(context);

        SubmissionHintResponse response = callAiWithRetryAndFallback(systemPrompt, userPrompt, requestedLevel, mistakeType);

        // Store generated hint
        SubmissionHint entity = new SubmissionHint(
                submissionId,
                userId,
                problem.getId(),
                response.hintLevel(),
                response.mistakeType(),
                response.hint(),
                response.concept(),
                response.revealsSolution()
        );
        hintRepository.save(entity);

        return response;
    }

    private SubmissionHintResponse callAiWithRetryAndFallback(String systemPrompt, String userPrompt, int level, MistakeType mistakeType) {
        // Attempt 1
        Optional<String> rawLlmResponse = mentorModelClient.generate(systemPrompt, userPrompt);
        SubmissionHintResponse parsed = tryParseLlmResponse(rawLlmResponse, level, mistakeType);
        if (parsed != null) {
            return parsed;
        }

        // Safe retry (Attempt 2) if malformed
        log.warn("Initial LLM response was malformed. Executing safe retry for level [{}]", level);
        Optional<String> retryResponse = mentorModelClient.generate(systemPrompt, userPrompt);
        parsed = tryParseLlmResponse(retryResponse, level, mistakeType);
        if (parsed != null) {
            return parsed;
        }

        // Controlled fallback if LLM is unavailable or persists in returning invalid JSON
        log.warn("LLM unavailable or malformed after retry. Returning controlled deterministic fallback for level [{}]", level);
        return buildControlledFallback(level, mistakeType);
    }

    private SubmissionHintResponse tryParseLlmResponse(Optional<String> rawOpt, int level, MistakeType mistakeType) {
        if (rawOpt.isEmpty()) {
            return null;
        }

        try {
            JsonNode json = objectMapper.readTree(rawOpt.get());
            String hint = json.has("guidance") ? json.get("guidance").asText() : (json.has("hint") ? json.get("hint").asText() : "");
            String concept = json.has("conceptKeywords") && json.get("conceptKeywords").isArray() && !json.get("conceptKeywords").isEmpty()
                    ? json.get("conceptKeywords").get(0).asText()
                    : (json.has("concept") ? json.get("concept").asText() : "Algorithmic Reasoning");

            if (hint.isBlank()) {
                return null;
            }

            // Anti-spoil validation: Redact any accidental solution code blocks at levels 1-3
            boolean revealsSolution = level >= 5;
            if (level <= 3) {
                hint = CODE_BLOCK_PATTERN.matcher(hint).replaceAll("[Guidance redacted code block to encourage self-solving]");
                revealsSolution = false;
            }

            return new SubmissionHintResponse(
                    mistakeType.name(),
                    level,
                    hint,
                    concept,
                    revealsSolution
            );
        } catch (Exception ex) {
            log.debug("Failed to parse LLM JSON hint payload: {}", ex.getMessage());
            return null;
        }
    }

    private SubmissionHintResponse buildControlledFallback(int level, MistakeType mistakeType) {
        String hint;
        String concept;
        boolean revealsSolution = false;

        switch (level) {
            case 1 -> {
                hint = "Trace your logic with a small 2-element test case. What value does your accumulator or pointer hold on the final iteration?";
                concept = "Loop Invariants & Boundary Conditions";
            }
            case 2 -> {
                hint = "Consider the fundamental data structure property. Can a lookup table (e.g. HashMap) or binary search reduce sequential scans?";
                concept = "Time Complexity & Fast Lookups";
            }
            case 3 -> {
                hint = "Examine how your function behaves when the input has duplicate values, all negative numbers, or is empty.";
                concept = "Edge Case Analysis";
            }
            case 4 -> {
                hint = "The issue occurs when the loop terminates before considering the final element or updates the state after checking termination.";
                concept = "Detailed State Transition Flow";
            }
            default -> {
                hint = "Initialize a fast tracking collection, iterate once while checking complementary criteria, and return when the condition matches.";
                concept = "Algorithmic Solution Blueprint";
                revealsSolution = true;
            }
        }

        return new SubmissionHintResponse(
                mistakeType.name(),
                level,
                hint,
                concept,
                revealsSolution
        );
    }

    private MistakeType deriveMistakeType(Submission submission) {
        if (submission.getVerdict() == null) {
            return MistakeType.UNKNOWN;
        }

        return switch (submission.getVerdict()) {
            case COMPILE_ERROR -> MistakeType.COMPILE_ERROR;
            case RUNTIME_ERROR -> {
                String err = submission.getErrorMessage();
                if (err != null && err.contains("NullPointer")) yield MistakeType.NULL_POINTER;
                if (err != null && err.contains("IndexOutOfBounds")) yield MistakeType.ARRAY_INDEX_OUT_OF_BOUNDS;
                yield MistakeType.RUNTIME_ERROR;
            }
            case TIME_LIMIT_EXCEEDED -> MistakeType.TIME_LIMIT;
            case MEMORY_LIMIT_EXCEEDED -> MistakeType.MEMORY_LIMIT;
            case WRONG_ANSWER -> MistakeType.LOGICAL_ERROR;
            case ACCEPTED -> MistakeType.NONE;
        };
    }

    private HintLevel mapToHintLevel(int level) {
        return switch (level) {
            case 1 -> HintLevel.LEVEL_1_NUDGE;
            case 2 -> HintLevel.LEVEL_2_CONCEPT;
            case 3 -> HintLevel.LEVEL_3_ALGORITHM;
            default -> HintLevel.LEVEL_4_TARGETED;
        };
    }
}
