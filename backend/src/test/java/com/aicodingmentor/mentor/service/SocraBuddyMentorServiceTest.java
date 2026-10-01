package com.aicodingmentor.mentor.service;

import com.aicodingmentor.mentor.dto.SocraBuddyDiagnosisRequest;
import com.aicodingmentor.mentor.dto.SocraBuddyDiagnosisResponse;
import com.aicodingmentor.mentor.dto.SocraBuddyHintRequest;
import com.aicodingmentor.mentor.dto.SocraBuddyHintResponse;
import com.aicodingmentor.mentor.model.HintLevel;
import com.aicodingmentor.mistake.model.MistakeClassification;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SocraBuddyMentorService Unit Tests")
class SocraBuddyMentorServiceTest {

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private MentorModelClient mentorModelClient;

    private SocraBuddyMentorService mentorService;
    private Problem sampleProblem;
    private final UUID problemId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        SocraBuddyPromptBuilder promptBuilder = new SocraBuddyPromptBuilder();
        mentorService = new SocraBuddyMentorServiceImpl(problemRepository, mentorModelClient, promptBuilder, objectMapper);

        sampleProblem = new Problem(
                "Two Sum",
                "two-sum",
                "Find indices of two numbers that add up to target.",
                ProblemDifficulty.EASY,
                "2 <= nums.length <= 10^4",
                "nums = [2,7,11,15], target = 9",
                "[0,1]",
                "Example 1",
                "class Solution {}",
                "JAVA",
                Set.of("Array", "Hash Table")
        );
        sampleProblem.setId(problemId);
    }

    @Test
    @DisplayName("Should generate Level 1 Gentle Nudge hint")
    void testGenerateHint_level1Nudge() {
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(sampleProblem));
        when(mentorModelClient.generate(any(), any())).thenReturn(Optional.empty()); // Fallback engine

        SocraBuddyHintRequest request = new SocraBuddyHintRequest(
                problemId,
                null,
                "class Solution { public int[] twoSum(int[] n, int t) { return null; } }",
                HintLevel.LEVEL_1_NUDGE,
                "Where do I start?",
                null
        );

        SocraBuddyHintResponse response = mentorService.generateHint(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.hintLevel()).isEqualTo(HintLevel.LEVEL_1_NUDGE);
        assertThat(response.guidance()).isNotEmpty();
        assertThat(response.socraticQuestion()).isNotEmpty();
        assertThat(response.guidance()).doesNotContain("return new int[]"); // Must not leak code
    }

    @Test
    @DisplayName("Should generate Level 2 Conceptual hint")
    void testGenerateHint_level2Concept() {
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(sampleProblem));
        when(mentorModelClient.generate(any(), any())).thenReturn(Optional.empty());

        SocraBuddyHintRequest request = new SocraBuddyHintRequest(
                problemId,
                null,
                "class Solution {}",
                HintLevel.LEVEL_2_CONCEPT,
                null,
                null
        );

        SocraBuddyHintResponse response = mentorService.generateHint(userId, request);

        assertThat(response.hintLevel()).isEqualTo(HintLevel.LEVEL_2_CONCEPT);
        assertThat(response.conceptKeywords()).contains("Time-Space Tradeoff");
    }

    @Test
    @DisplayName("Should generate Level 3 Algorithmic Blueprint hint")
    void testGenerateHint_level3Algorithm() {
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(sampleProblem));
        when(mentorModelClient.generate(any(), any())).thenReturn(Optional.empty());

        SocraBuddyHintRequest request = new SocraBuddyHintRequest(
                problemId,
                null,
                "class Solution {}",
                HintLevel.LEVEL_3_ALGORITHM,
                null,
                null
        );

        SocraBuddyHintResponse response = mentorService.generateHint(userId, request);

        assertThat(response.hintLevel()).isEqualTo(HintLevel.LEVEL_3_ALGORITHM);
        assertThat(response.guidance()).contains("Step 1:");
    }

    @Test
    @DisplayName("Should generate Level 4 Targeted Pointer hint")
    void testGenerateHint_level4Targeted() {
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(sampleProblem));
        when(mentorModelClient.generate(any(), any())).thenReturn(Optional.empty());

        SocraBuddyHintRequest request = new SocraBuddyHintRequest(
                problemId,
                null,
                "class Solution {}",
                HintLevel.LEVEL_4_TARGETED,
                null,
                null
        );

        SocraBuddyHintResponse response = mentorService.generateHint(userId, request);

        assertThat(response.hintLevel()).isEqualTo(HintLevel.LEVEL_4_TARGETED);
        assertThat(response.guidance()).contains("loop");
    }

    @Test
    @DisplayName("Should sanitize and redact full code blocks from AI response to enforce Socratic policy")
    void testGenerateHint_redactsAccidentalCodeBlocksFromLlm() {
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(sampleProblem));
        when(mentorModelClient.getModelName()).thenReturn("qwen2.5-coder:latest");

        String llmOutput = """
                {
                  "title": "Think about HashMaps",
                  "guidance": "Here is what you need: ```java\\nHashMap<Integer, Integer> map = new HashMap<>();\\nreturn new int[]{0, 1};\\n``` Try this.",
                  "socraticQuestion": "What is the complement?",
                  "conceptKeywords": ["HashMap"]
                }
                """;
        when(mentorModelClient.generate(any(), any())).thenReturn(Optional.of(llmOutput));

        SocraBuddyHintRequest request = new SocraBuddyHintRequest(
                problemId,
                null,
                "class Solution {}",
                HintLevel.LEVEL_2_CONCEPT,
                null,
                null
        );

        SocraBuddyHintResponse response = mentorService.generateHint(userId, request);

        assertThat(response.guidance()).doesNotContain("HashMap<Integer, Integer> map = new HashMap<>()");
        assertThat(response.guidance()).contains("redacted by SocraBuddy");
    }

    @Test
    @DisplayName("Should diagnose runtime NullPointerException correctly")
    void testDiagnoseMistake_nullPointer() {
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(sampleProblem));
        when(mentorModelClient.generate(any(), any())).thenReturn(Optional.empty());

        SocraBuddyDiagnosisRequest request = new SocraBuddyDiagnosisRequest(
                problemId,
                null,
                "class Solution {}",
                null,
                "Exception in thread 'main' java.lang.NullPointerException: Cannot invoke method",
                1
        );

        SocraBuddyDiagnosisResponse response = mentorService.diagnoseMistake(userId, request);

        assertThat(response.mistakeClassification()).isEqualTo(MistakeClassification.NULL_POINTER);
        assertThat(response.recommendedHintLevel()).isEqualTo(HintLevel.LEVEL_4_TARGETED);
    }
}
