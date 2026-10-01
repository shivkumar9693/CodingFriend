package com.aicodingmentor.mentor.service;

import com.aicodingmentor.mentor.dto.AiPromptContext;
import com.aicodingmentor.mentor.dto.ExecutionMetricsContext;
import com.aicodingmentor.mentor.dto.SocraBuddyDiagnosisRequest;
import com.aicodingmentor.mentor.dto.SocraBuddyHintRequest;
import com.aicodingmentor.mentor.model.HintLevel;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SocraBuddyPromptBuilder & Context Builder Unit Tests")
class SocraBuddyPromptBuilderTest {

    private SocraBuddyPromptBuilder promptBuilder;
    private PromptSanitizer promptSanitizer;
    private Problem sampleProblem;

    @BeforeEach
    void setUp() {
        promptSanitizer = new PromptSanitizer();
        promptBuilder = new SocraBuddyPromptBuilder(promptSanitizer);

        sampleProblem = new Problem(
                "Two Sum",
                "two-sum",
                "Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.",
                ProblemDifficulty.EASY,
                "2 <= nums.length <= 10^4",
                "int[] nums, int target",
                "int[]",
                "nums = [2,7,11,15], target = 9 -> [0, 1]",
                "class Solution { public int[] twoSum(int[] nums, int target) {} }",
                "JAVA",
                java.util.Set.of("ARRAY", "HASH_TABLE")
        );
    }

    @Test
    @DisplayName("Should build full contextual prompt with metrics, previous hints, and mistake trends")
    void testBuildContextualPrompt_fullContext() {
        AiPromptContext context = AiPromptContext.builder()
                .problem(sampleProblem)
                .sourceCode("class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        return null;\n    }\n}")
                .programmingLanguage("JAVA")
                .hintLevel(HintLevel.LEVEL_2_CONCEPT)
                .mistakeType("NULL_POINTER")
                .compilerOrRuntimeError("NullPointerException at line 3")
                .failingTestSummary("Failed test case #1: expected [0, 1] but got null")
                .executionMetrics(ExecutionMetricsContext.of(45L, 33554432L, 1))
                .previousHints(List.of(
                        "Level 1: Consider what data structure can remember previous values.",
                        "Level 1: Trace loop with small array [2, 7]."
                ))
                .learnerMistakeHistory(List.of(
                        "Learner has encountered 3 NULL_POINTER mistakes in previous submissions",
                        "Learner frequently misses single-element edge cases"
                ))
                .userQuestion("How do I avoid returning null when no pair is found?")
                .build();

        String prompt = promptBuilder.buildContextualPrompt(context);

        // Verify Problem Information
        assertThat(prompt).contains("Two Sum");
        assertThat(prompt).contains("2 <= nums.length <= 10^4");
        assertThat(prompt).contains("EASY");

        // Verify Learner Code
        assertThat(prompt).contains("```java");
        assertThat(prompt).contains("public int[] twoSum");

        // Verify Diagnostics & Metrics
        assertThat(prompt).contains("DIAGNOSED MISTAKE TYPE:\nNULL_POINTER");
        assertThat(prompt).contains("NullPointerException at line 3");
        assertThat(prompt).contains("Execution Time: 45ms");
        assertThat(prompt).contains("Peak Memory: 32768 KB");

        // Verify Previous Hints & History
        assertThat(prompt).contains("PREVIOUS HINTS GIVEN TO LEARNER (DO NOT REPEAT THESE):");
        assertThat(prompt).contains("1. Level 1: Consider what data structure");
        assertThat(prompt).contains("2. Level 1: Trace loop with small array");
        assertThat(prompt).contains("LEARNER'S RECURRING MISTAKE PATTERNS:");
        assertThat(prompt).contains("3 NULL_POINTER mistakes");

        // Verify Level 2 Specific Instructions
        assertThat(prompt).contains("REQUESTED HINT LEVEL: LEVEL_2_CONCEPT");
        assertThat(prompt).contains("HINT TIER 2 (CONCEPTUAL HINT)");

        // Verify Version Header
        assertThat(prompt).contains(promptBuilder.getPromptVersion());
    }

    @Test
    @DisplayName("Should sanitize sensitive tokens and credentials from learner code and error messages")
    void testBuildContextualPrompt_sanitizesSecrets() {
        String unsafeCode = """
                class Solution {
                    String apiKey = "gsk_mockSecretKeyForPromptBuilderTesting1234567890";
                    String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
                    String pass = "db_password: 'superSecret123'";
                }
                """;

        AiPromptContext context = AiPromptContext.builder()
                .problem(sampleProblem)
                .sourceCode(unsafeCode)
                .compilerOrRuntimeError("Connection failed to jdbc:mysql://root:rootpass@localhost:3306/db")
                .hintLevel(HintLevel.LEVEL_1_NUDGE)
                .build();

        String prompt = promptBuilder.buildContextualPrompt(context);

        assertThat(prompt).doesNotContain("gsk_mockSecretKeyForPromptBuilderTesting1234567890");
        assertThat(prompt).contains("[REDACTED_API_KEY]");

        assertThat(prompt).doesNotContain("eyJhbGciOiJIUzI1NiIs");
        assertThat(prompt).contains("[REDACTED_JWT]");

        assertThat(prompt).doesNotContain("superSecret123");
        assertThat(prompt).contains("[REDACTED_SECRET]");

        assertThat(prompt).doesNotContain("jdbc:mysql://root:rootpass@localhost:3306/db");
        assertThat(prompt).contains("[REDACTED_DB_URL]");
    }

    @Test
    @DisplayName("Should build backward compatible hint user prompt from SocraBuddyHintRequest")
    void testBuildHintUserPrompt() {
        SocraBuddyHintRequest request = new SocraBuddyHintRequest(
                UUID.randomUUID(),
                null,
                "class Solution {}",
                HintLevel.LEVEL_3_ALGORITHM,
                "Need algorithmic clue",
                null
        );

        String prompt = promptBuilder.buildHintUserPrompt(sampleProblem, request);

        assertThat(prompt).contains("Two Sum");
        assertThat(prompt).contains("HINT TIER 3 (ALGORITHMIC BLUEPRINT)");
        assertThat(prompt).contains("REQUESTED HINT LEVEL: LEVEL_3_ALGORITHM");
    }

    @Test
    @DisplayName("Should build versioned system prompts")
    void testBuildSystemPrompt_versioned() {
        String v1 = promptBuilder.buildSystemPrompt("v1");
        String v2 = promptBuilder.buildSystemPrompt("v2");

        assertThat(v1).contains("You are SocraBuddy, an expert Socratic AI Coding Mentor");
        assertThat(v2).contains("You are SocraBuddy (v2), an advanced Socratic AI Coding Mentor");
    }
}
