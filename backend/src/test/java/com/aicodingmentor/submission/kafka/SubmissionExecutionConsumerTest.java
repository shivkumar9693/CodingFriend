package com.aicodingmentor.submission.kafka;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.execution.model.SubmissionEvaluationResult;
import com.aicodingmentor.execution.service.TestCaseEvaluator;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.problem.domain.TestCase;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.aicodingmentor.problem.service.TestCaseService;
import com.aicodingmentor.submission.domain.Submission;
import com.aicodingmentor.submission.domain.SubmissionStatus;
import com.aicodingmentor.submission.event.CodeSubmissionEvent;
import com.aicodingmentor.submission.event.ExecutionResultEvent;
import com.aicodingmentor.submission.repository.SubmissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmissionExecutionConsumer Unit Tests")
class SubmissionExecutionConsumerTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private TestCaseService testCaseService;

    @Mock
    private TestCaseEvaluator testCaseEvaluator;

    @Mock
    private SubmissionEventProducer eventProducer;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private com.aicodingmentor.user.service.UserService userService;

    private ObjectMapper objectMapper;
    private SubmissionExecutionConsumer consumer;

    private UUID submissionId;
    private UUID userId;
    private UUID problemId;
    private Problem sampleProblem;
    private Submission sampleSubmission;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new SubmissionExecutionConsumer(
                submissionRepository,
                problemRepository,
                testCaseService,
                testCaseEvaluator,
                eventProducer,
                objectMapper,
                cacheManager,
                userService
        );

        submissionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        problemId = UUID.randomUUID();

        sampleProblem = new Problem(
                "Two Sum", "two-sum", "Desc", ProblemDifficulty.EASY,
                "", "", "", "", "", "JAVA", Set.of("ARRAY")
        );
        sampleProblem.setId(problemId);

        sampleSubmission = new Submission(
                userId,
                problemId,
                "class Solution {}",
                "JAVA"
        );
    }

    @Test
    @DisplayName("Idempotency guard: Ignores duplicate event if submission is already COMPLETED")
    void testConsumeSubmission_AlreadyCompleted_SkipsDuplicateExecution() {
        sampleSubmission.markCompleted(EvaluationVerdict.ACCEPTED, 5, 5, 50L, null);

        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(sampleSubmission));

        CodeSubmissionEvent event = new CodeSubmissionEvent(
                submissionId, userId, problemId, "two-sum", "class Solution {}", "JAVA", Instant.now()
        );

        consumer.consumeSubmission(event);

        verify(testCaseEvaluator, never()).evaluate(anyString(), anyString(), any(), anyLong(), anyInt(), anyString());
        verify(eventProducer, never()).sendExecutionResult(any());
    }

    @Test
    @DisplayName("Executes sandbox evaluation and publishes ExecutionResultEvent")
    void testConsumeSubmission_Success() {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(sampleSubmission));
        when(testCaseService.getTestCasesForExecution(problemId)).thenReturn(List.of(new TestCase("[1]", "1", false, 1, "")));

        SubmissionEvaluationResult evalResult = new SubmissionEvaluationResult(
                EvaluationVerdict.ACCEPTED, 1, 1, 35L, null, List.of()
        );
        when(testCaseEvaluator.evaluate(anyString(), anyString(), any(), anyLong(), anyInt(), anyString()))
                .thenReturn(evalResult);
        when(submissionRepository.save(any(Submission.class))).thenReturn(sampleSubmission);

        CodeSubmissionEvent event = new CodeSubmissionEvent(
                submissionId, userId, problemId, "two-sum", "class Solution {}", "JAVA", Instant.now()
        );

        consumer.consumeSubmission(event);

        ArgumentCaptor<ExecutionResultEvent> captor = ArgumentCaptor.forClass(ExecutionResultEvent.class);
        verify(eventProducer).sendExecutionResult(captor.capture());

        ExecutionResultEvent publishedEvent = captor.getValue();
        assertThat(publishedEvent.submissionId()).isEqualTo(sampleSubmission.getId());
        assertThat(publishedEvent.verdict()).isEqualTo(EvaluationVerdict.ACCEPTED);
        assertThat(publishedEvent.testsPassed()).isEqualTo(1);
    }
}
