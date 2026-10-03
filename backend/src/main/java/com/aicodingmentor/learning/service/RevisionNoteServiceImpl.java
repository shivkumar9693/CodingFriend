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
                        Analyze the learner's submitted code against the failed test case counter-example.
                        Do NOT give away full replacement code. Instead, generate a structured dry-run comparison.
                        You must respond strictly with valid JSON conforming to this schema:
                        {
                          "mistakeCategory": "LOGIC_ERROR | OFF_BY_ONE | CORNER_CASE | TIME_LIMIT_EXCEEDED | RUNTIME_EXCEPTION | SYNTAX_ERROR",
                          "conceptSummary": "Concise 1-sentence diagnostic explanation of where logic diverged",
                          "failedInput": "Counter-example input string",
                          "actualOutput": "Learner's output",
                          "expectedOutput": "Expected output",
                          "studentFlowTrace": "Step-by-step dry run trace of what student's code executed line-by-line",
                          "correctFlowTrace": "Step-by-step trace of how the invariant/correct algorithm reaches expected output",
                          "keyTakeaway": "1-2 memorable invariant rules to prevent repeating this mistake",
                          "relatedConcepts": ["Concept1", "Concept2"]
                        }
                        """;

                String userPrompt = String.format("""
                        Problem: %s
                        Language: %s
                        Verdict: %s
                        Failed Counter-Example Input: %s
                        Actual Output: %s
                        Expected Output: %s
                        Error Log: %s

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
                    String summary = node.has("conceptSummary") ? node.get("conceptSummary").asText() : "Logic discrepancy identified in test case evaluation.";
                    String failedIn = node.has("failedInput") ? node.get("failedInput").asText() : input;
                    String actOut = node.has("actualOutput") ? node.get("actualOutput").asText() : actual;
                    String expOut = node.has("expectedOutput") ? node.get("expectedOutput").asText() : expected;
                    String sFlow = node.has("studentFlowTrace") ? node.get("studentFlowTrace").asText() : "";
                    String cFlow = node.has("correctFlowTrace") ? node.get("correctFlowTrace").asText() : "";
                    String takeaway = node.has("keyTakeaway") ? node.get("keyTakeaway").asText() : "Always verify invariant conditions against edge cases.";

                    List<String> concepts = new ArrayList<>();
                    if (node.has("relatedConcepts") && node.get("relatedConcepts").isArray()) {
                        for (JsonNode cn : node.get("relatedConcepts")) {
                            concepts.add(cn.asText());
                        }
                    }

                    if (!sFlow.isBlank() && !cFlow.isBlank()) {
                        return new PostMortemAnalysisResponse(
                                category, summary, failedIn, actOut, expOut, sFlow, cFlow, takeaway,
                                concepts.isEmpty() ? List.of("Logic Tracing", "Edge Cases") : concepts
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

        boolean isCompileError = "COMPILE_ERROR".equalsIgnoreCase(verdict)
                || "COMPILATION_ERROR".equalsIgnoreCase(verdict)
                || "SYNTAX_ERROR".equalsIgnoreCase(verdict)
                || (errorMessage != null && (errorMessage.contains("error:") || errorMessage.contains("javac") || errorMessage.contains("compile")));

        if (isCompileError) {
            category = "SYNTAX_ERROR";
            String diag = (errorMessage != null && !errorMessage.isBlank()) ? errorMessage : actual;
            String lineNum = "source";
            String errorDetail = "syntax or type signature error";
            String codeSnippet = "";

            // Extract line number and compiler error description
            java.util.regex.Pattern linePattern = java.util.regex.Pattern.compile("(?:MainDriver|Solution|[a-zA-Z0-9_]+)\\.java:(\\d+):\\s*error:\\s*([^\n\r]+)");
            java.util.regex.Matcher matcher = linePattern.matcher(diag);
            if (matcher.find()) {
                lineNum = "line " + matcher.group(1);
                errorDetail = matcher.group(2).trim();
            } else if (diag.contains("error:")) {
                int errIdx = diag.indexOf("error:");
                errorDetail = diag.substring(errIdx + 6).split("\n")[0].trim();
            }

            // Extract code snippet if present
            if (diag.contains("error:")) {
                String[] parts = diag.split("\n");
                for (String p : parts) {
                    if (!p.contains(".java:") && !p.contains("error:") && !p.trim().isEmpty() && !p.contains("^")) {
                        codeSnippet = p.trim();
                        break;
                    }
                }
            }

            summary = String.format("Compiler Syntax Error (%s): %s%s",
                    lineNum,
                    errorDetail,
                    codeSnippet.isBlank() ? "." : " at `" + codeSnippet + "`."
            );
            input = "Java 17 Compiler (Syntax Validation)";
            actual = String.format("%s: %s%s", lineNum, errorDetail, codeSnippet.isBlank() ? "" : " [" + codeSnippet + "]");
            expected = "0 Compilation Errors (Clean Bytecode Build)";

            sFlow = String.format(
                    "• Step 1: javac compiler initiated AST tokenization and syntax parsing.\n" +
                    "• Step 2: Parser encountered syntax anomaly on %s: %s\n" +
                    "• Step 3: Compilation halted: %s",
                    lineNum,
                    codeSnippet.isBlank() ? errorDetail : codeSnippet,
                    errorDetail
            );

            cFlow = String.format(
                    "• Step 1: Verify balanced delimiters (parentheses `()`, brackets `[]`, braces `{}`).\n" +
                    "• Step 2: Ensure method arguments and expressions are closed before terminating with `;`.\n" +
                    "• Step 3: Code parses cleanly into valid JVM bytecode with 0 compilation errors."
            );

            takeaway = "Always balance closing delimiters `)` and `]` before terminating statements with a semicolon `;`.";
            concepts.addAll(List.of("Syntax Rules", "Compiler Diagnostics", "Delimiter Balancing"));
        } else if ("TIME_LIMIT_EXCEEDED".equalsIgnoreCase(verdict) || (code.contains("for") && code.contains("while") && code.length() > 500)) {
            category = "TIME_LIMIT_EXCEEDED";
            summary = "Execution timed out due to polynomial time complexity $O(N^2)$ or unpruned nested loops on large scale inputs.";
            sFlow = "• Step 1: Input size $N$ received.\n• Step 2: Outer loop scans from $0$ to $N-1$.\n• Step 3: Nested inner loop performs redundant duplicate comparisons $O(N^2)$ iterations.\n• Outcome: CPU quantum exhausted (Execution Time > 2000ms).";
            cFlow = "• Step 1: Hash/Set or Two-Pointer preprocessing achieves $O(N)$ or $O(N \\log N)$ traversal.\n• Step 2: Lookups performed in $O(1)$ amortized time.\n• Outcome: Clean termination within 5ms.";
            takeaway = "When constraints exceed $10^4$, replace nested brute-force loops with a HashMap lookup or Two-Pointer invariant.";
            concepts.addAll(List.of("Time Complexity", "Space-Time Tradeoff", "HashMap Invariants"));
        } else if ("RUNTIME_ERROR".equalsIgnoreCase(verdict) || (errorMessage != null && (errorMessage.contains("Exception") || errorMessage.contains("Error")))) {
            if (errorMessage != null && errorMessage.contains("NullPointer")) {
                category = "RUNTIME_EXCEPTION";
                summary = "NullPointerException triggered by dereferencing an uninitialized reference or missing map key.";
                sFlow = String.format("• Step 1: Evaluated input: %s\n• Step 2: Attempted method call on null object reference.\n• Step 3: JVM threw NullPointerException.", input);
                cFlow = "• Step 1: Check for null before invoking methods or use Optional/containsKey guards.\n• Step 2: Safe execution path taken.\n• Outcome: Clean execution yielding expected output.";
                takeaway = "Always guard object references with null checks or use `containsKey` before accessing map keys.";
                concepts.addAll(List.of("Null Safety", "Defensive Programming", "Object Initialization"));
            } else if (errorMessage != null && errorMessage.contains("IndexOutOfBounds")) {
                category = "OFF_BY_ONE";
                summary = "Array index accessed out of allowable bounds (`i <= length` instead of `i < length` or negative index).";
                sFlow = String.format("• Step 1: Evaluated input %s.\n• Step 2: Loop boundary reached index equal to array length.\n• Step 3: Attempted direct memory read at invalid offset.\n• Outcome: Runtime exception triggered (`%s`).", input, actual);
                cFlow = String.format("• Step 1: Guard condition `i < array.length` or strict interval check applied.\n• Step 2: Safely processes elements within [0, N-1].\n• Outcome: Yields valid expected output `%s`.", expected);
                takeaway = "0-indexed collections span indices $0$ to $N-1$. Use strict inequality `<` or `.length - 1` for terminal bounds.";
                concepts.addAll(List.of("Boundary Conditions", "Array Bounds", "Loop Invariants"));
            } else {
                category = "RUNTIME_EXCEPTION";
                summary = String.format("Runtime exception occurred during execution: %s", errorMessage != null ? errorMessage.split("\n")[0] : "Unexpected error");
                sFlow = String.format("• Step 1: Processing input: %s\n• Step 2: Execution path encountered an unhandled exception.\n• Step 3: Fault: %s", input, errorMessage != null ? errorMessage : actual);
                cFlow = String.format("• Step 1: Apply input validation and guard edge constraints.\n• Step 2: Execute algorithm within safe boundaries.\n• Step 3: Successfully produces: %s", expected);
                takeaway = "Test boundary conditions such as 0, empty inputs, single-element collections, and negative numbers.";
                concepts.addAll(List.of("Runtime Invariants", "Exception Handling", "Edge Case Safety"));
            }
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
