package com.aicodingmentor.submission.kafka;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.learning.service.LearnerMistakeTrackingService;
import com.aicodingmentor.mistake.model.MistakeAnalysis;
import com.aicodingmentor.mistake.model.MistakeSeverity;
import com.aicodingmentor.mistake.model.MistakeType;
import com.aicodingmentor.mistake.service.AiMistakeClassifier;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.aicodingmentor.submission.domain.Submission;
import com.aicodingmentor.submission.event.AiAnalysisEvent;
import com.aicodingmentor.submission.event.ExecutionResultEvent;
import com.aicodingmentor.submission.event.LearningEvent;
import com.aicodingmentor.submission.repository.SubmissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutionResultConsumer Unit Tests")
class ExecutionResultConsumerTest {

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private AiMistakeClassifier mistakeClassifier;

    @Mock
    private LearnerMistakeTrackingService mistakeTrackingService;

    @Mock
    private SubmissionEventProducer eventProducer;

    private ObjectMapper objectMapper;
    private ExecutionResultConsumer consumer;

    private UUID submissionId;
    private UUID userId;
    private UUID problemId;
    private Problem sampleProblem;
    private Submission sampleSubmission;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new ExecutionResultConsumer(
                problemRepository,
                submissionRepository,
                mistakeClassifier,
                mistakeTrackingService,
                eventProducer,
                objectMapper
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
    @DisplayName("Accepted submissions skip mistake classification")
    void testConsumeExecutionResult_Accepted_SkipsClassification() {
        ExecutionResultEvent event = new ExecutionResultEvent(
                submissionId, userId, problemId, EvaluationVerdict.ACCEPTED, 5, 5, 40L, null, Instant.now()
        );

        consumer.consumeExecutionResult(event);

        verify(mistakeClassifier, never()).classify(any(), any(), any());
        verify(mistakeTrackingService, never()).recordMistake(any(), any(), any());
    }

    @Test
    @DisplayName("Failed submissions trigger mistake classification and publish learning events")
    void testConsumeExecutionResult_Failed_TriggersClassificationAndTracking() {
        ExecutionResultEvent event = new ExecutionResultEvent(
                submissionId, userId, problemId, EvaluationVerdict.WRONG_ANSWER, 2, 5, 60L, null, Instant.now()
        );

        when(problemRepository.findById(problemId)).thenReturn(Optional.of(sampleProblem));
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(sampleSubmission));

        MistakeAnalysis analysis = MistakeAnalysis.of(
                MistakeType.OFF_BY_ONE, "Off by one loop boundary", "Loop condition exceeds array length",
                MistakeSeverity.MEDIUM, 0.85
        );
        when(mistakeClassifier.classify(any(), any(), any())).thenReturn(analysis);

        consumer.consumeExecutionResult(event);

        verify(mistakeTrackingService).recordMistake(eq(userId), eq(MistakeType.OFF_BY_ONE), eq(Set.of("ARRAY")));

        ArgumentCaptor<AiAnalysisEvent> aiCaptor = ArgumentCaptor.forClass(AiAnalysisEvent.class);
        verify(eventProducer).sendAiAnalysis(aiCaptor.capture());
        assertThat(aiCaptor.getValue().mistakeType()).isEqualTo(MistakeType.OFF_BY_ONE);
        assertThat(aiCaptor.getValue().confidence()).isEqualTo(0.85);

        ArgumentCaptor<LearningEvent> learningCaptor = ArgumentCaptor.forClass(LearningEvent.class);
        verify(eventProducer).sendLearningEvent(learningCaptor.capture());
        assertThat(learningCaptor.getValue().mistakeType()).isEqualTo(MistakeType.OFF_BY_ONE);
    }
}
