package com.aicodingmentor.mentor.service;

import com.aicodingmentor.common.error.ResourceNotFoundException;
import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.mentor.dto.SocraBuddyDiagnosisRequest;
import com.aicodingmentor.mentor.dto.SocraBuddyDiagnosisResponse;
import com.aicodingmentor.mentor.dto.SocraBuddyHintRequest;
import com.aicodingmentor.mentor.dto.SocraBuddyHintResponse;
import com.aicodingmentor.mentor.model.HintLevel;
import com.aicodingmentor.mistake.model.MistakeClassification;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SocraBuddyMentorServiceImpl implements SocraBuddyMentorService {

    private static final Logger log = LoggerFactory.getLogger(SocraBuddyMentorServiceImpl.class);
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[a-zA-Z]*\\n[\\s\\S]*?```");

    private final ProblemRepository problemRepository;
    private final MentorModelClient mentorModelClient;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public SocraBuddyMentorServiceImpl(
            ProblemRepository problemRepository,
            MentorModelClient mentorModelClient,
            PromptBuilder promptBuilder,
            ObjectMapper objectMapper
    ) {
        this.problemRepository = problemRepository;
        this.mentorModelClient = mentorModelClient;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public SocraBuddyHintResponse generateHint(UUID userId, SocraBuddyHintRequest request) {
        Problem problem = problemRepository.findById(request.problemId())
                .orElseThrow(() -> new ResourceNotFoundException("Problem", request.problemId()));

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildHintUserPrompt(problem, request);

        Optional<String> rawLlmResponse = mentorModelClient.generate(systemPrompt, userPrompt);

        if (rawLlmResponse.isPresent()) {
            try {
                JsonNode json = objectMapper.readTree(rawLlmResponse.get());
                String title = json.has("title") ? json.get("title").asText() : "SocraBuddy's Insight";
                String guidance = json.has("guidance") ? json.get("guidance").asText() : "";
                String question = json.has("socraticQuestion") ? json.get("socraticQuestion").asText() : "What is the expected behavior for your edge cases?";

                List<String> keywords = new ArrayList<>();
                if (json.has("conceptKeywords") && json.get("conceptKeywords").isArray()) {
                    for (JsonNode kw : json.get("conceptKeywords")) {
                        keywords.add(kw.asText());
                    }
                }

                // Rule 30: Validate and sanitize LLM response - strip any accidental complete code blocks
                guidance = sanitizeNonSpoilerGuidance(guidance);

                return SocraBuddyHintResponse.of(
                        request.hintLevel(),
                        title,
                        guidance,
                        question,
                        keywords,
                        mentorModelClient.getModelName()
                );
            } catch (Exception ex) {
                log.warn("Failed to parse mentor model JSON response, falling back to rule engine", ex);
            }
        }

        // Rule-Based Socratic Fallback
        return generateFallbackHint(problem, request);
    }

    @Override
    @Transactional(readOnly = true)
    public SocraBuddyDiagnosisResponse diagnoseMistake(UUID userId, SocraBuddyDiagnosisRequest request) {
        Problem problem = problemRepository.findById(request.problemId())
                .orElseThrow(() -> new ResourceNotFoundException("Problem", request.problemId()));

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildDiagnosisPrompt(problem, request);

        Optional<String> rawLlmResponse = mentorModelClient.generate(systemPrompt, userPrompt);

        if (rawLlmResponse.isPresent()) {
            try {
                JsonNode json = objectMapper.readTree(rawLlmResponse.get());
                MistakeClassification classification = MistakeClassification.GENERAL_BUG;
                if (json.has("mistakeClassification")) {
                    try {
                        classification = MistakeClassification.valueOf(json.get("mistakeClassification").asText().toUpperCase());
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                String summary = json.has("diagnosisSummary") ? json.get("diagnosisSummary").asText() : "Execution did not match expected output.";
                String guidance = json.has("socraticGuidance") ? json.get("socraticGuidance").asText() : "Inspect the condition where your loop or calculation terminates.";
                String action = json.has("recommendedAction") ? json.get("recommendedAction").asText() : "Trace with sample inputs on paper.";
                HintLevel nextLevel = HintLevel.LEVEL_1_NUDGE;
                if (json.has("recommendedHintLevel")) {
                    try {
                        nextLevel = HintLevel.valueOf(json.get("recommendedHintLevel").asText().toUpperCase());
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                return new SocraBuddyDiagnosisResponse(
                        classification,
                        summary,
                        guidance,
                        action,
                        nextLevel
                );
            } catch (Exception ex) {
                log.warn("Failed to parse Ollama diagnosis response, falling back to rule diagnosis", ex);
            }
        }

        return generateFallbackDiagnosis(problem, request);
    }

    private String sanitizeNonSpoilerGuidance(String guidance) {
        if (guidance == null) return "";
        // If the AI accidentally returned large markdown code blocks, strip them to protect Socratic integrity
        if (CODE_BLOCK_PATTERN.matcher(guidance).find()) {
            return CODE_BLOCK_PATTERN.matcher(guidance).replaceAll("*(Code block redacted by SocraBuddy: Explore the concept rather than copying code)*");
        }
        return guidance;
    }

    private SocraBuddyHintResponse generateFallbackHint(Problem problem, SocraBuddyHintRequest request) {
        String problemTitle = problem.getTitle();
        return switch (request.hintLevel()) {
            case LEVEL_1_NUDGE -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_1_NUDGE,
                    "Examining Invariants & Edge Cases",
                    "Consider the fundamental boundaries of '" + problemTitle + "'. How does your current code behave when given extreme inputs such as an empty structure, a single element, or repeating numbers?",
                    "What assumptions are you making about the ordering or values of the input?",
                    List.of("Edge Cases", "Invariants", problemTitle),
                    "SocraBuddy Socratic Heuristics"
            );
            case LEVEL_2_CONCEPT -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_2_CONCEPT,
                    "Core Data Structure & Time Complexity",
                    "To achieve an optimal solution for '" + problemTitle + "', think about trade-offs between time and space. Can a fast lookup structure (like a HashMap or HashSet) or a two-pointer technique reduce a nested scan?",
                    "If you store previously seen elements in memory, what can you check in O(1) time?",
                    List.of("Time-Space Tradeoff", "Fast Lookup", "Pointers"),
                    "SocraBuddy Socratic Heuristics"
            );
            case LEVEL_3_ALGORITHM -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_3_ALGORITHM,
                    "Algorithmic Blueprint Breakdown",
                    "Step 1: Initialize your tracking data structure.\nStep 2: Iterate through the elements while computing the required complement or invariant.\nStep 3: If the target state is met, return the result immediately; otherwise update your tracking state.\nStep 4: Handle the default/fallback case after iteration completes.",
                    "How does each step transition state toward the final result?",
                    List.of("Algorithm Flow", "State Transition", "Iteration"),
                    "SocraBuddy Socratic Heuristics"
            );
            case LEVEL_4_TARGETED -> SocraBuddyHintResponse.of(
                    HintLevel.LEVEL_4_TARGETED,
                    "Targeted Logic Pointer",
                    "Look closely at the condition inside your primary loop and how indices/complements are updated. Verify whether you are returning before checking all candidates or accessing an element after modifying the collection.",
                    "Does your loop condition `i < length` or pointer check skip or duplicate the final element?",
                    List.of("Loop Termination", "Pointer Update", "Boundary"),
                    "SocraBuddy Socratic Heuristics"
            );
        };
    }

    private SocraBuddyDiagnosisResponse generateFallbackDiagnosis(Problem problem, SocraBuddyDiagnosisRequest request) {
        String error = request.errorMessage() != null ? request.errorMessage() : "";
        EvaluationVerdict verdict = request.verdict() != null ? request.verdict() : EvaluationVerdict.WRONG_ANSWER;

        if (verdict == EvaluationVerdict.COMPILE_ERROR || error.contains("error:")) {
            return new SocraBuddyDiagnosisResponse(
                    MistakeClassification.SYNTAX_ERROR,
                    "Java compilation failed due to syntax or type mismatch.",
                    "Review compiler messages carefully for line numbers and unmatched tokens.",
                    "Check semicolons, braces, variable declarations, and return types.",
                    HintLevel.LEVEL_1_NUDGE
            );
        }

        if (verdict == EvaluationVerdict.TIME_LIMIT_EXCEEDED) {
            return new SocraBuddyDiagnosisResponse(
                    MistakeClassification.TIME_LIMIT_EXCEEDED,
                    "Execution exceeded maximum allowable time limit.",
                    "Your algorithm may have an infinite loop or a time complexity that is too high (e.g. O(N^2) instead of O(N)).",
                    "Ensure loop index variables are advancing and look for ways to optimize nested iterations.",
                    HintLevel.LEVEL_2_CONCEPT
            );
        }

        if (error.contains("ArithmeticException")) {
            return new SocraBuddyDiagnosisResponse(
                    MistakeClassification.GENERAL_BUG,
                    "Arithmetic error occurred during runtime (likely division or modulo by zero).",
                    "What happens when the divisor evaluates to zero in your mathematical expressions?",
                    "Add a guard check before performing division or modulo operations.",
                    HintLevel.LEVEL_1_NUDGE
            );
        }

        if (error.contains("NullPointerException")) {
            return new SocraBuddyDiagnosisResponse(
                    MistakeClassification.NULL_POINTER,
                    "Null reference was dereferenced at runtime.",
                    "Which object was accessed before ensuring it was non-null?",
                    "Check map lookups or uninitialized array elements before calling methods on them.",
                    HintLevel.LEVEL_4_TARGETED
            );
        }

        if (error.contains("IndexOutOfBoundsException")) {
            return new SocraBuddyDiagnosisResponse(
                    MistakeClassification.INDEX_OUT_OF_BOUNDS,
                    "Array or list index exceeded the valid range [0, length - 1].",
                    "Check your loop boundary condition. Are you accessing `arr[i]` when `i == arr.length`?",
                    "Change loop bounds to strict `< length` instead of `<= length`.",
                    HintLevel.LEVEL_4_TARGETED
            );
        }

        return new SocraBuddyDiagnosisResponse(
                MistakeClassification.LOGIC_INCORRECT,
                "Output diverged from expected solution on test case evaluation.",
                "Your logic produced an incorrect result for the specific input pattern in this test.",
                "Trace the variable values step-by-step with the sample input on paper.",
                HintLevel.LEVEL_2_CONCEPT
        );
    }
}
