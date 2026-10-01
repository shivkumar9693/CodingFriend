package com.aicodingmentor.mentor.service;

import com.aicodingmentor.common.error.ApiException;
import com.aicodingmentor.learning.service.LearnerMistakeTrackingService;
import com.aicodingmentor.mentor.domain.SubmissionHint;
import com.aicodingmentor.mentor.dto.SubmissionHintRequest;
import com.aicodingmentor.mentor.dto.SubmissionHintResponse;
import com.aicodingmentor.mentor.repository.SubmissionHintRepository;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.aicodingmentor.submission.domain.Submission;
import com.aicodingmentor.submission.repository.SubmissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmissionHintService Unit Tests")
class SubmissionHintServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private SubmissionHintRepository hintRepository;

    @Mock
    private MentorModelClient mentorModelClient;

    @Mock
    private PromptBuilder promptBuilder;

    @Mock
    private PromptSanitizer promptSanitizer;

    @Mock
    private LearnerMistakeTrackingService mistakeTrackingService;

    private ObjectMapper objectMapper;
    private SubmissionHintServiceImpl hintService;

    private UUID userId;
    private UUID submissionId;
    private UUID problemId;
    private Problem sampleProblem;
    private Submission sampleSubmission;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        userId = UUID.randomUUID();
        submissionId = UUID.randomUUID();
        problemId = UUID.randomUUID();

        sampleProblem = new Problem(
                "Two Sum",
                "two-sum",
                "Find two numbers that add up to target.",
                ProblemDifficulty.EASY,
                "2 <= nums.length <= 10^4",
                "int[] nums, int target",
                "int[]",
                "nums = [2,7], target = 9",
                "class Solution {}",
                "JAVA",
                Set.of("ARRAY")
        );
        sampleProblem.setId(problemId);

        sampleSubmission = new Submission(
                userId,
                problemId,
                "class Solution { public int[] twoSum(int[] nums, int target) { return null; } }",
                "JAVA"
        );
        sampleSubmission.setVerdict(com.aicodingmentor.execution.model.EvaluationVerdict.WRONG_ANSWER);
        sampleSubmission.setExecutionTimeMs(30L);

        hintService = new SubmissionHintServiceImpl(
                submissionRepository,
                problemRepository,
                hintRepository,
                mentorModelClient,
                promptBuilder,
                promptSanitizer,
                mistakeTrackingService,
                objectMapper
        );
    }

    @Test
    @DisplayName("Should generate Level 1 hint successfully and persist to database")
    void testGenerateSubmissionHint_level1_success() {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(sampleSubmission));
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(sampleProblem));
        when(hintRepository.findFirstBySubmissionIdAndHintLevelOrderByCreatedAtDesc(submissionId, 1)).thenReturn(Optional.empty());
        when(hintRepository.findBySubmissionIdOrderByHintLevelAsc(submissionId)).thenReturn(List.of());
        when(promptBuilder.buildSystemPrompt()).thenReturn("System prompt");
        when(promptBuilder.buildContextualPrompt(any())).thenReturn("User prompt");

        String aiResponse = """
                {
                  "guidance": "What happens on the boundary when i reaches nums.length?",
                  "conceptKeywords": ["Loop Bounds", "Array Indexing"]
                }
                """;
        when(mentorModelClient.generate(anyString(), anyString())).thenReturn(Optional.of(aiResponse));

        SubmissionHintResponse response = hintService.generateSubmissionHint(
                userId,
                submissionId,
                new SubmissionHintRequest(1)
        );

        assertThat(response).isNotNull();
        assertThat(response.hintLevel()).isEqualTo(1);
        assertThat(response.hint()).contains("What happens on the boundary");
        assertThat(response.concept()).isEqualTo("Loop Bounds");
        assertThat(response.revealsSolution()).isFalse();

        verify(hintRepository, times(1)).save(any(SubmissionHint.class));
        verify(mistakeTrackingService, times(1)).recordMistake(eq(userId), any(), any());
    }

    @Test
    @DisplayName("Should prevent requesting Level 4 before earlier exploratory hints (progression guard)")
    void testProgressionGuard_level4WithoutPreviousHints_throwsBadRequest() {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(sampleSubmission));
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(sampleProblem));
        when(hintRepository.findFirstBySubmissionIdAndHintLevelOrderByCreatedAtDesc(submissionId, 4)).thenReturn(Optional.empty());
        when(hintRepository.findBySubmissionIdOrderByHintLevelAsc(submissionId)).thenReturn(List.of());

        assertThatThrownBy(() -> hintService.generateSubmissionHint(userId, submissionId, new SubmissionHintRequest(4)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Progression constraint");

        verify(mentorModelClient, never()).generate(anyString(), anyString());
    }

    @Test
    @DisplayName("Should allow Level 4 hint after level 1-3 hint has been explored")
    void testProgressionGuard_level4WithPreviousHint_allowed() {
        SubmissionHint prevHint = new SubmissionHint(
                submissionId, userId, problemId, 1, "OFF_BY_ONE", "Check your loop bounds.", "Invariants", false
        );

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(sampleSubmission));
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(sampleProblem));
        when(hintRepository.findFirstBySubmissionIdAndHintLevelOrderByCreatedAtDesc(submissionId, 4)).thenReturn(Optional.empty());
        when(hintRepository.findBySubmissionIdOrderByHintLevelAsc(submissionId)).thenReturn(List.of(prevHint));
        when(promptBuilder.buildSystemPrompt()).thenReturn("System prompt");
        when(promptBuilder.buildContextualPrompt(any())).thenReturn("User prompt");

        String aiResponse = """
                {
                  "guidance": "The loop terminates at i < length - 1, skipping the final pair evaluation.",
                  "conceptKeywords": ["Off By One"]
                }
                """;
        when(mentorModelClient.generate(anyString(), anyString())).thenReturn(Optional.of(aiResponse));

        SubmissionHintResponse response = hintService.generateSubmissionHint(
                userId,
                submissionId,
                new SubmissionHintRequest(4)
        );

        assertThat(response.hintLevel()).isEqualTo(4);
        assertThat(response.hint()).contains("skipping the final pair");
    }

    @Test
    @DisplayName("Should throw ApiException with FORBIDDEN when accessing another user's submission")
    void testUnauthorizedSubmissionAccess() {
        UUID otherUserId = UUID.randomUUID();
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(sampleSubmission));

        assertThatThrownBy(() -> hintService.generateSubmissionHint(otherUserId, submissionId, new SubmissionHintRequest(1)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("Should return controlled deterministic fallback if AI is unavailable or malformed")
    void testControlledFallbackWhenAiUnavailable() {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(sampleSubmission));
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(sampleProblem));
        when(hintRepository.findFirstBySubmissionIdAndHintLevelOrderByCreatedAtDesc(submissionId, 1)).thenReturn(Optional.empty());
        when(hintRepository.findBySubmissionIdOrderByHintLevelAsc(submissionId)).thenReturn(List.of());
        when(promptBuilder.buildSystemPrompt()).thenReturn("System prompt");
        when(promptBuilder.buildContextualPrompt(any())).thenReturn("User prompt");
        when(mentorModelClient.generate(anyString(), anyString())).thenReturn(Optional.empty());

        SubmissionHintResponse response = hintService.generateSubmissionHint(
                userId,
                submissionId,
                new SubmissionHintRequest(1)
        );

        assertThat(response).isNotNull();
        assertThat(response.hintLevel()).isEqualTo(1);
        assertThat(response.hint()).contains("Trace your logic with a small");
        assertThat(response.revealsSolution()).isFalse();
    }
}
