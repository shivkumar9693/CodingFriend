package com.aicodingmentor.mentor.service;

import com.aicodingmentor.mentor.dto.AiPromptContext;
import com.aicodingmentor.mentor.dto.ExecutionMetricsContext;
import com.aicodingmentor.mentor.dto.SocraBuddyDiagnosisRequest;
import com.aicodingmentor.mentor.dto.SocraBuddyHintRequest;
import com.aicodingmentor.mentor.model.HintLevel;
import com.aicodingmentor.problem.domain.Problem;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default implementation of {@link PromptBuilder} (SocraBuddy Socratic Prompt Builder).
 * Sanitizes all context against credential leaks and constructs versioned Socratic prompts.
 */
@Component
public class SocraBuddyPromptBuilder implements PromptBuilder {

    public static final String CURRENT_PROMPT_VERSION = "v1.4.0-socratic-context";

    private static final String SOCRABUDDY_SYSTEM_PROMPT_V1 = """
            You are SocraBuddy, an expert Socratic AI Coding Mentor on the CodingFriend platform.
            Your mission is to guide learners to solve coding challenges on their own through inquiry and targeted conceptual insight.

            CRITICAL CARDINAL RULES:
            1. NEVER write the complete solution code or complete replacement functions.
            2. NEVER give away the direct answer or copy-paste fixes.
            3. Guide the learner using the Socratic method (guiding questions, algorithmic clues, and conceptual pointers).
            4. Respond in valid JSON format only with keys: "title", "guidance", "socraticQuestion", "conceptKeywords".
            """;

    private static final String SOCRABUDDY_SYSTEM_PROMPT_V2 = """
            You are SocraBuddy (v2), an advanced Socratic AI Coding Mentor.
            You possess deep pedagogical expertise across Java, Python, C++, and JavaScript.

            CARDINAL RULES:
            1. STRICT ANTI-SPOIL: Never write complete function implementations or solve the challenge directly.
            2. MULTI-LEVEL SOCRATIC PROGRESSION: Tailor your response strictly to the requested hint tier.
            3. EMPOWERMENT: Encourage independent debugging by highlighting invariants, state transitions, and edge conditions.
            4. STRICT JSON OUTPUT: Return only a valid JSON payload with "title", "guidance", "socraticQuestion", and "conceptKeywords".
            """;

    private final PromptSanitizer promptSanitizer;

    public SocraBuddyPromptBuilder() {
        this(new PromptSanitizer());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SocraBuddyPromptBuilder(PromptSanitizer promptSanitizer) {
        this.promptSanitizer = promptSanitizer != null ? promptSanitizer : new PromptSanitizer();
    }

    @Override
    public String buildSystemPrompt() {
        return SOCRABUDDY_SYSTEM_PROMPT_V1;
    }

    @Override
    public String buildSystemPrompt(String version) {
        if ("v2".equalsIgnoreCase(version)) {
            return SOCRABUDDY_SYSTEM_PROMPT_V2;
        }
        return SOCRABUDDY_SYSTEM_PROMPT_V1;
    }

    @Override
    public String buildHintUserPrompt(Problem problem, SocraBuddyHintRequest request) {
        AiPromptContext context = AiPromptContext.builder()
                .problem(problem)
                .sourceCode(request.sourceCode())
                .programmingLanguage("JAVA")
                .hintLevel(request.hintLevel())
                .compilerOrRuntimeError(request.lastErrorMessage())
                .userQuestion(request.userQuestion())
                .build();

        return buildContextualPrompt(context);
    }

    @Override
    public String buildDiagnosisPrompt(Problem problem, SocraBuddyDiagnosisRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("PROMPT VERSION: ").append(CURRENT_PROMPT_VERSION).append("\n\n");
        sb.append("PROBLEM DETAILS:\n");
        sb.append("Title: ").append(promptSanitizer.sanitize(problem.getTitle())).append("\n");
        sb.append("Description: ").append(promptSanitizer.sanitize(problem.getDescription())).append("\n\n");

        sb.append("LEARNER'S SUBMITTED CODE:\n```java\n");
        sb.append(promptSanitizer.sanitize(request.sourceCode())).append("\n```\n\n");

        sb.append("EXECUTION OUTCOME:\n");
        sb.append("Verdict: ").append(request.verdict() != null ? request.verdict().name() : "FAILED").append("\n");
        if (request.errorMessage() != null && !request.errorMessage().isBlank()) {
            sb.append("Error details: ").append(promptSanitizer.sanitize(request.errorMessage())).append("\n");
        }
        if (request.failedTestNumber() != null) {
            sb.append("Failed on test case #").append(request.failedTestNumber()).append("\n");
        }

        sb.append("\nDiagnose the bug without writing the fix. Respond ONLY with JSON:\n");
        sb.append("{\n");
        sb.append("  \"mistakeClassification\": \"SYNTAX_ERROR|OFF_BY_ONE|NULL_POINTER|INDEX_OUT_OF_BOUNDS|INFINITE_LOOP|TIME_LIMIT_EXCEEDED|OUTPUT_FORMAT_MISMATCH|LOGIC_INCORRECT|EDGE_CASE_MISSED|GENERAL_BUG\",\n");
        sb.append("  \"diagnosisSummary\": \"One sentence explaining what went wrong\",\n");
        sb.append("  \"socraticGuidance\": \"Socratic question directing them to the flaw\",\n");
        sb.append("  \"recommendedAction\": \"Action step (e.g. trace loop with i=0)\",\n");
        sb.append("  \"recommendedHintLevel\": \"LEVEL_1_NUDGE|LEVEL_2_CONCEPT|LEVEL_3_ALGORITHM|LEVEL_4_TARGETED\"\n");
        sb.append("}\n");

        return sb.toString();
    }

    @Override
    public String buildContextualPrompt(AiPromptContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("PROMPT VERSION: ").append(CURRENT_PROMPT_VERSION).append("\n\n");

        // 1. Problem Statement & Constraints
        if (context.problem() != null) {
            sb.append("PROBLEM DETAILS:\n");
            sb.append("Title: ").append(promptSanitizer.sanitize(context.problem().getTitle())).append("\n");
            sb.append("Difficulty: ").append(context.problem().getDifficulty()).append("\n");
            sb.append("Description: ").append(promptSanitizer.sanitize(context.problem().getDescription())).append("\n");
            if (context.problem().getConstraints() != null && !context.problem().getConstraints().isBlank()) {
                sb.append("Constraints: ").append(promptSanitizer.sanitize(context.problem().getConstraints())).append("\n");
            }
            sb.append("\n");
        }

        // 2. Learner Code (Sanitized)
        String lang = context.programmingLanguage() != null ? context.programmingLanguage().toLowerCase() : "java";
        sb.append("LEARNER'S CURRENT CODE (").append(lang.toUpperCase()).append("):\n");
        sb.append("```").append(lang).append("\n");
        sb.append(promptSanitizer.sanitize(context.sourceCode())).append("\n```\n\n");

        // 3. Mistake Classification (if present)
        if (context.mistakeType() != null && !context.mistakeType().isBlank()) {
            sb.append("DIAGNOSED MISTAKE TYPE:\n");
            sb.append(promptSanitizer.sanitize(context.mistakeType())).append("\n\n");
        }

        // 4. Compiler / Runtime Error Output (Sanitized)
        if (context.compilerOrRuntimeError() != null && !context.compilerOrRuntimeError().isBlank()) {
            sb.append("ERROR / STACKTRACE OBSERVED:\n");
            sb.append(promptSanitizer.sanitize(context.compilerOrRuntimeError())).append("\n\n");
        }

        // 5. Failing Test Information (Sanitized - Never Hidden Test Inputs)
        if (context.failingTestSummary() != null && !context.failingTestSummary().isBlank()) {
            sb.append("TEST FAILURE CONTEXT:\n");
            sb.append(promptSanitizer.sanitize(context.failingTestSummary())).append("\n\n");
        }

        // 6. Execution Metrics
        if (context.executionMetrics() != null) {
            ExecutionMetricsContext metrics = context.executionMetrics();
            sb.append("EXECUTION METRICS:\n");
            if (metrics.executionTimeMs() != null) {
                sb.append("- Execution Time: ").append(metrics.executionTimeMs()).append("ms\n");
            }
            if (metrics.memoryUsageBytes() != null) {
                sb.append("- Peak Memory: ").append(metrics.memoryUsageBytes() / 1024).append(" KB\n");
            }
            if (metrics.exitCode() != null) {
                sb.append("- Exit Code: ").append(metrics.exitCode()).append("\n");
            }
            sb.append("\n");
        }

        // 7. Previous Hints Given in Session
        if (context.previousHints() != null && !context.previousHints().isEmpty()) {
            sb.append("PREVIOUS HINTS GIVEN TO LEARNER (DO NOT REPEAT THESE):\n");
            List<String> hints = context.previousHints();
            for (int i = 0; i < hints.size(); i++) {
                sb.append(i + 1).append(". ").append(promptSanitizer.sanitize(hints.get(i))).append("\n");
            }
            sb.append("\n");
        }

        // 8. Learner Mistake History Trends
        if (context.learnerMistakeHistory() != null && !context.learnerMistakeHistory().isEmpty()) {
            sb.append("LEARNER'S RECURRING MISTAKE PATTERNS:\n");
            for (String mistake : context.learnerMistakeHistory()) {
                sb.append("- ").append(promptSanitizer.sanitize(mistake)).append("\n");
            }
            sb.append("\n");
        }

        // 9. Specific Learner Question
        if (context.userQuestion() != null && !context.userQuestion().isBlank()) {
            sb.append("LEARNER'S SPECIFIC QUESTION / CONFUSION:\n");
            sb.append(promptSanitizer.sanitize(context.userQuestion())).append("\n\n");
        }

        // 10. Hint Tier Instructions
        HintLevel level = context.hintLevel() != null ? context.hintLevel() : HintLevel.LEVEL_1_NUDGE;
        sb.append("REQUESTED HINT LEVEL: ").append(level.name()).append("\n");
        sb.append(getLevelSpecificInstructions(level));

        // 11. Strict JSON Response Schema
        sb.append("\n\nRespond ONLY with a JSON object adhering to this schema:\n");
        sb.append("{\n");
        sb.append("  \"title\": \"Short engaging hint title\",\n");
        sb.append("  \"guidance\": \"Socratic explanation strictly matching the requested hint tier\",\n");
        sb.append("  \"socraticQuestion\": \"A thought-provoking question to prompt critical reasoning\",\n");
        sb.append("  \"conceptKeywords\": [\"Keyword1\", \"Keyword2\"]\n");
        sb.append("}\n");

        return sb.toString();
    }

    @Override
    public String getPromptVersion() {
        return CURRENT_PROMPT_VERSION;
    }

    private String getLevelSpecificInstructions(HintLevel level) {
        return switch (level) {
            case LEVEL_1_NUDGE -> """
                    HINT TIER 1 (GENTLE NUDGE):
                    - Do NOT explain the full algorithm or identify the exact bug line.
                    - Ask an observational question about edge cases (e.g., empty collection, zeros, negative numbers, single element).
                    - Encourage the learner to verify their assumptions.
                    """;
            case LEVEL_2_CONCEPT -> """
                    HINT TIER 2 (CONCEPTUAL HINT):
                    - Explain the fundamental computer science concept or invariant.
                    - Highlight the relationship between the data structure properties and the problem constraints.
                    """;
            case LEVEL_3_ALGORITHM -> """
                    HINT TIER 3 (ALGORITHMIC BLUEPRINT):
                    - Outline the step-by-step logic in plain English steps (Step 1, Step 2, Step 3).
                    - Do NOT write source code blocks. Describe the algorithm structure conceptually.
                    """;
            case LEVEL_4_TARGETED -> """
                    HINT TIER 4 (TARGETED POINTER):
                    - Point to the specific loop, variable, condition, or operation that caused the issue.
                    - Explain WHY that specific condition behaves unexpectedly without rewriting the code for them.
                    """;
        };
    }
}
