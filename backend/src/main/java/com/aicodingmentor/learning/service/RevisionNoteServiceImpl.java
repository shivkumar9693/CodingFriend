package com.aicodingmentor.learning.service;

import com.aicodingmentor.common.error.ResourceNotFoundException;
import com.aicodingmentor.learning.domain.MasteryStatus;
import com.aicodingmentor.learning.domain.MistakeCategory;
import com.aicodingmentor.learning.domain.RevisionNote;
import com.aicodingmentor.learning.dto.CreateRevisionNoteRequest;
import com.aicodingmentor.learning.dto.GeneratePostMortemRequest;
import com.aicodingmentor.learning.dto.PostMortemAnalysisResponse;
import com.aicodingmentor.learning.dto.RevisionNoteResponse;
import com.aicodingmentor.learning.dto.RevisionNotesSummaryDto;
import com.aicodingmentor.learning.dto.UpdateRevisionNoteRequest;
import com.aicodingmentor.learning.repository.RevisionNoteRepository;
import com.aicodingmentor.mentor.service.MentorModelClient;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RevisionNoteServiceImpl implements RevisionNoteService {

    private static final Logger log = LoggerFactory.getLogger(RevisionNoteServiceImpl.class);

    private final RevisionNoteRepository revisionNoteRepository;
    private final ProblemRepository problemRepository;
    private final MentorModelClient mentorModelClient;
    private final ObjectMapper objectMapper;

    public RevisionNoteServiceImpl(
            RevisionNoteRepository revisionNoteRepository,
            ProblemRepository problemRepository,
            MentorModelClient mentorModelClient,
            ObjectMapper objectMapper
    ) {
        this.revisionNoteRepository = revisionNoteRepository;
        this.problemRepository = problemRepository;
        this.mentorModelClient = mentorModelClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public PostMortemAnalysisResponse generatePostMortem(UUID userId, GeneratePostMortemRequest request) {
        String problemTitle = request.problemTitle() != null ? request.problemTitle() : "Coding Challenge";
        String language = request.language() != null ? request.language() : "JAVA";
        String code = request.submittedCode() != null ? request.submittedCode() : "";
        String input = (request.failedInput() != null && !request.failedInput().isBlank()) ? request.failedInput() : "Sample Test Case";
        String actual = (request.actualOutput() != null && !request.actualOutput().isBlank()) ? request.actualOutput() : "Incorrect / Empty Output";
        String expected = (request.expectedOutput() != null && !request.expectedOutput().isBlank()) ? request.expectedOutput() : "Expected Spec Output";
        String verdict = request.verdict() != null ? request.verdict() : "WRONG_ANSWER";

        // Try AI Mentor generation first if LLM is available
        if (mentorModelClient != null && mentorModelClient.isAvailable()) {
            try {
                String systemPrompt = """
                        You are an expert algorithmic mentor and pedagogical tutor.
                        Analyze the learner's submitted code against the error log or failed counter-example test case.
                        Do NOT provide full solution code replacement. Instead, generate an insightful, structured dry-run diagnosis.
                        If the verdict is COMPILE_ERROR or has an Error Log, analyze the exact syntax/type error on that line and explain the syntax contract.
                        You must respond strictly with valid JSON conforming to this schema:
                        {
                          "mistakeCategory": "SYNTAX_ERROR | LOGIC_ERROR | OFF_BY_ONE | CORNER_CASE | TIME_LIMIT_EXCEEDED | RUNTIME_EXCEPTION",
                          "conceptSummary": "Concise 1-sentence diagnostic explanation of where syntax or logic diverged",
                          "failedInput": "Counter-example input string or Compilation Phase",
                          "actualOutput": "Learner's output or compiler error message",
                          "expectedOutput": "Expected output or clean compilation",
                          "studentFlowTrace": "Step-by-step trace of what student's code executed or where compiler failed",
                          "correctFlowTrace": "Step-by-step trace of how the invariant or correct syntax resolves the issue",
                          "keyTakeaway": "1-2 memorable invariant rules to prevent repeating this mistake",
                          "relatedConcepts": ["Concept1", "Concept2"]
                        }
                        """;

                String userPrompt = String.format("""
                        Problem: %s
                        Language: %s
                        Verdict: %s
                        Failed Input / Phase: %s
                        Actual Output / Error: %s
                        Expected Invariant: %s
                        Compiler / Runtime Error Log: %s

                        Submitted Code:
                        %s
                        """,
                        problemTitle, language, verdict, input, actual, expected,
                        request.errorMessage() != null ? request.errorMessage() : "None",
                        code
                );

                Optional<String> llmOutput = mentorModelClient.generate(systemPrompt, userPrompt);
                if (llmOutput.isPresent()) {
                    JsonNode node = objectMapper.readTree(llmOutput.get());
                    String category = node.has("mistakeCategory") ? node.get("mistakeCategory").asText() : "LOGIC_ERROR";
                    String summary = node.has("conceptSummary") ? node.get("conceptSummary").asText() : "Discrepancy identified in evaluation.";
                    String failedIn = node.has("failedInput") ? node.get("failedInput").asText() : input;
                    String actOut = node.has("actualOutput") ? node.get("actualOutput").asText() : actual;
                    String expOut = node.has("expectedOutput") ? node.get("expectedOutput").asText() : expected;
                    String sFlow = node.has("studentFlowTrace") ? node.get("studentFlowTrace").asText() : "";
                    String cFlow = node.has("correctFlowTrace") ? node.get("correctFlowTrace").asText() : "";
                    String takeaway = node.has("keyTakeaway") ? node.get("keyTakeaway").asText() : "Always verify invariant conditions before submitting.";

                    List<String> concepts = new ArrayList<>();
                    if (node.has("relatedConcepts") && node.get("relatedConcepts").isArray()) {
                        for (JsonNode cn : node.get("relatedConcepts")) {
                            concepts.add(cn.asText());
                        }
                    }

                    if (!sFlow.isBlank() && !cFlow.isBlank()) {
                        return new PostMortemAnalysisResponse(
                                category, summary, failedIn, actOut, expOut, sFlow, cFlow, takeaway,
                                concepts.isEmpty() ? List.of("Logic Tracing", "Syntax Invariants") : concepts
                        );
                    }
                }
            } catch (Exception e) {
                log.warn("AI LLM post-mortem generation encountered parsing error, falling back to deterministic heuristic: {}", e.getMessage());
            }
        }

        // High-precision Deterministic Heuristic Post-Mortem Generator
        return generateDeterministicPostMortem(problemTitle, language, code, verdict, input, actual, expected, request.errorMessage());
    }

    private PostMortemAnalysisResponse generateDeterministicPostMortem(
            String problemTitle,
            String language,
            String code,
            String verdict,
            String input,
            String actual,
            String expected,
            String errorMessage
    ) {
        String category = "LOGIC_ERROR";
        String summary;
        String sFlow;
        String cFlow;
        String takeaway;
        List<String> concepts = new ArrayList<>();

        if ("COMPILE_ERROR".equalsIgnoreCase(verdict) || "COMPILATION_ERROR".equalsIgnoreCase(verdict) || (errorMessage != null && errorMessage.contains("error:"))) {
            category = "SYNTAX_ERROR";
            String cleanErr = (errorMessage != null && !errorMessage.isBlank())
                    ? errorMessage.replaceAll("(?m)^/tmp/[^:]+:", "").trim()
                    : "Syntax or type signature mismatch in code";
            if (cleanErr.length() > 150) {
                cleanErr = cleanErr.substring(0, 150) + "...";
            }

            summary = "Syntax or compiler diagnostic encountered during compilation phase.";
            input = "Java 17 Compilation Phase";
            actual = cleanErr;
            expected = "Clean compilation with zero syntax errors";
            sFlow = String.format("• Step 1: Java compiler parsed the source file.\n• Step 2: Diagnostic encountered: %s\n• Step 3: Compilation halted before bytecode generation.", cleanErr);
            cFlow = "• Step 1: Ensure all opening brackets `(`, `{`, `[` have corresponding closing pairs `)`, `}`, `]`.\n• Step 2: Verify all statements end with `;` and method arguments match declared types.\n• Step 3: Compiler succeeds and generates valid executable bytecode.";
            takeaway = "Double check matching parentheses and semicolons on function calls and collection methods before running.";
            concepts.addAll(List.of("Syntax Invariants", "Expression Grammar", "Compiler Diagnostics"));
        } else if ("TIME_LIMIT_EXCEEDED".equalsIgnoreCase(verdict) || (code.contains("for") && code.contains("while") && code.length() > 500)) {
            category = "TIME_LIMIT_EXCEEDED";
            summary = "Execution timed out due to polynomial time complexity $O(N^2)$ or unpruned nested loops on large scale inputs.";
            sFlow = "• Step 1: Input size $N$ received.\n• Step 2: Outer loop scans from $0$ to $N-1$.\n• Step 3: Nested inner loop performs redundant duplicate comparisons $O(N^2)$ iterations.\n• Outcome: CPU quantum exhausted (Execution Time > 2000ms).";
            cFlow = "• Step 1: Hash/Set or Two-Pointer preprocessing achieves $O(N)$ or $O(N \\log N)$ traversal.\n• Step 2: Lookups performed in $O(1)$ amortized time.\n• Outcome: Clean termination within 5ms.";
            takeaway = "When constraints exceed $10^4$, replace nested brute-force loops with a HashMap lookup or Two-Pointer invariant.";
            concepts.addAll(List.of("Time Complexity", "Space-Time Tradeoff", "HashMap Invariants"));
        } else if ("RUNTIME_ERROR".equalsIgnoreCase(verdict) || (errorMessage != null && errorMessage.contains("IndexOutOfBounds"))) {
            category = "OFF_BY_ONE";
            summary = "Array index accessed out of allowable bounds (`i <= length` instead of `i < length` or negative index).";
            sFlow = String.format("• Step 1: Evaluated input %s.\n• Step 2: Loop boundary reached index equal to array length.\n• Step 3: Attempted direct memory read at invalid offset.\n• Outcome: Runtime exception triggered (`%s`).", input, actual);
            cFlow = String.format("• Step 1: Guard condition `i < array.length` or strict interval check applied.\n• Step 2: Safely processes elements within [0, N-1].\n• Outcome: Yields valid expected output `%s`.", expected);
            takeaway = "0-indexed collections span indices $0$ to $N-1$. Use strict inequality `<` or `.length - 1` for terminal bounds.";
            concepts.addAll(List.of("Boundary Conditions", "Array Bounds", "Loop Invariants"));
        } else {
            // General Logic / Counter-Example Discrepancy
            category = "LOGIC_ERROR";
            summary = String.format("Logic diverged on input `%s`, producing `%s` when `%s` was expected.", input, actual, expected);
            sFlow = String.format("• Step 1: Processing input: %s\n• Step 2: Branching condition or accumulator mutated state prematurely.\n• Step 3: Final returned value evaluated to: %s\n• Discrepancy: Failed to satisfy problem condition.", input, actual);
            cFlow = String.format("• Step 1: Given input: %s\n• Step 2: Apply the problem invariant systematically (e.g. check complement, preserve ordering).\n• Step 3: Correct state yields expected output: %s", input, expected);
            takeaway = "Trace code step-by-step with small edge cases (e.g., negative numbers, duplicates, empty inputs) before submitting.";
            concepts.addAll(List.of("Algorithmic Invariants", "Edge Case Analysis", "State Mutation"));
        }

        return new PostMortemAnalysisResponse(
                category, summary, input, actual, expected, sFlow, cFlow, takeaway, concepts
        );
    }

    @Override
    @Transactional
    public RevisionNoteResponse createNote(UUID userId, CreateRevisionNoteRequest request) {
        String problemSlug = request.problemSlug();
        if ((problemSlug == null || problemSlug.isBlank()) && request.problemId() != null) {
            Optional<Problem> problem = problemRepository.findById(request.problemId());
            if (problem.isPresent()) {
                problemSlug = problem.get().getSlug();
            }
        }

        MistakeCategory category = MistakeCategory.fromString(request.mistakeCategory());

        RevisionNote note = new RevisionNote(
                userId,
                request.problemId(),
                request.submissionId(),
                request.problemTitle(),
                problemSlug,
                request.language(),
                category,
                request.conceptSummary(),
                request.failedInput(),
                request.actualOutput(),
                request.expectedOutput(),
                request.studentFlowTrace(),
                request.correctFlowTrace(),
                request.keyTakeaway(),
                request.submittedCode(),
                request.userReflections()
        );

        if (Boolean.TRUE.equals(request.isBookmarked())) {
            note.setBookmarked(true);
        }

        RevisionNote saved = revisionNoteRepository.save(note);
        return RevisionNoteResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RevisionNoteResponse> getUserNotes(UUID userId, MasteryStatus status, Boolean bookmarked, String search) {
        List<RevisionNote> notes;

        if (search != null && !search.isBlank()) {
            notes = revisionNoteRepository.searchUserNotes(userId, search.trim());
        } else if (Boolean.TRUE.equals(bookmarked)) {
            notes = revisionNoteRepository.findByUserIdAndIsBookmarkedTrueOrderByCreatedAtDesc(userId);
        } else if (status != null) {
            notes = revisionNoteRepository.findByUserIdAndMasteryStatusOrderByCreatedAtDesc(userId, status);
        } else {
            notes = revisionNoteRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }

        return notes.stream()
                .map(RevisionNoteResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RevisionNoteResponse getNoteById(UUID userId, UUID noteId) {
        RevisionNote note = revisionNoteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RevisionNote", noteId));
        return RevisionNoteResponse.from(note);
    }

    @Override
    @Transactional
    public RevisionNoteResponse updateNote(UUID userId, UUID noteId, UpdateRevisionNoteRequest request) {
        RevisionNote note = revisionNoteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RevisionNote", noteId));

        if (request.userReflections() != null) {
            note.setUserReflections(request.userReflections());
        }
        if (request.masteryStatus() != null) {
            note.setMasteryStatus(request.masteryStatus());
        }
        if (request.isBookmarked() != null) {
            note.setBookmarked(request.isBookmarked());
        }

        RevisionNote updated = revisionNoteRepository.save(note);
        return RevisionNoteResponse.from(updated);
    }

    @Override
    @Transactional
    public void deleteNote(UUID userId, UUID noteId) {
        RevisionNote note = revisionNoteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RevisionNote", noteId));
        revisionNoteRepository.delete(note);
    }

    @Override
    @Transactional
    public RevisionNoteResponse markReviewed(UUID userId, UUID noteId) {
        RevisionNote note = revisionNoteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("RevisionNote", noteId));

        note.incrementReview();
        RevisionNote saved = revisionNoteRepository.save(note);
        return RevisionNoteResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RevisionNotesSummaryDto getSummary(UUID userId) {
        long total = revisionNoteRepository.countByUserId(userId);
        long toRevise = revisionNoteRepository.countByUserIdAndMasteryStatus(userId, MasteryStatus.TO_REVISE);
        long revised = revisionNoteRepository.countByUserIdAndMasteryStatus(userId, MasteryStatus.REVISED);
        long mastered = revisionNoteRepository.countByUserIdAndMasteryStatus(userId, MasteryStatus.MASTERED);
        long bookmarked = revisionNoteRepository.countByUserIdAndIsBookmarkedTrue(userId);
        long dueToday = revisionNoteRepository.countDueForReview(userId, Instant.now());

        List<RevisionNote> allUserNotes = revisionNoteRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<String, Long> categoryMap = new HashMap<>();
        for (RevisionNote n : allUserNotes) {
            String cat = n.getMistakeCategory() != null ? n.getMistakeCategory().name() : "LOGIC_ERROR";
            categoryMap.put(cat, categoryMap.getOrDefault(cat, 0L) + 1);
        }

        return new RevisionNotesSummaryDto(
                total,
                toRevise,
                revised,
                mastered,
                bookmarked,
                dueToday,
                categoryMap
        );
    }
}
