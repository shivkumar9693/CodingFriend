package com.aicodingmentor.submission.kafka;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.execution.model.SubmissionEvaluationResult;
import com.aicodingmentor.execution.model.TestCaseEvaluationResult;
import com.aicodingmentor.learning.service.LearnerMistakeTrackingService;
import com.aicodingmentor.mistake.model.MistakeAnalysis;
import com.aicodingmentor.mistake.model.MistakeType;
import com.aicodingmentor.mistake.service.AiMistakeClassifier;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.aicodingmentor.submission.domain.Submission;
import com.aicodingmentor.submission.event.AiAnalysisEvent;
import com.aicodingmentor.submission.event.ExecutionResultEvent;
import com.aicodingmentor.submission.event.LearningEvent;
import com.aicodingmentor.submission.repository.SubmissionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import static com.aicodingmentor.submission.kafka.KafkaTopicConfig.TOPIC_EXECUTION_RESULTS;

/**
 * Consumer for 'execution-results' topic.
 * Triggers asynchronous AI mistake analysis and updates learner mistake tracking stats.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class ExecutionResultConsumer {

    private static final Logger log = LoggerFactory.getLogger(ExecutionResultConsumer.class);

    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final AiMistakeClassifier mistakeClassifier;
    private final LearnerMistakeTrackingService mistakeTrackingService;
    private final SubmissionEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    public ExecutionResultConsumer(
            ProblemRepository problemRepository,
            SubmissionRepository submissionRepository,
            AiMistakeClassifier mistakeClassifier,
            LearnerMistakeTrackingService mistakeTrackingService,
            SubmissionEventProducer eventProducer,
            ObjectMapper objectMapper
    ) {
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
        this.mistakeClassifier = mistakeClassifier;
        this.mistakeTrackingService = mistakeTrackingService;
        this.eventProducer = eventProducer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = TOPIC_EXECUTION_RESULTS, groupId = "execution-result-group")
    public void consumeExecutionResult(ExecutionResultEvent event) {
        if (event == null || event.submissionId() == null) {
            return;
        }

        log.info("Processing ExecutionResultEvent for submission [{}] with verdict [{}]",
                event.submissionId(), event.verdict());

        if (event.verdict() == EvaluationVerdict.ACCEPTED) {
            log.info("Submission [{}] passed all test cases. Skipping mistake classification.", event.submissionId());
            return;
        }

        Problem problem = problemRepository.findById(event.problemId()).orElse(null);
        Submission submission = submissionRepository.findById(event.submissionId()).orElse(null);

        if (problem == null || submission == null) {
            log.warn("Problem or Submission entity not found for result event [{}].", event.submissionId());
            return;
        }

        // Parse test case details if available
        List<TestCaseEvaluationResult> testCaseResults = List.of();
        if (submission.getEvaluationDetails() != null && !submission.getEvaluationDetails().isBlank()) {
            try {
                testCaseResults = objectMapper.readValue(submission.getEvaluationDetails(), new TypeReference<>() {});
            } catch (Exception ex) {
                log.warn("Failed to parse test case results JSON for submission [{}]", submission.getId(), ex);
            }
        }

        SubmissionEvaluationResult evalResult = new SubmissionEvaluationResult(
                event.verdict(),
                event.testsPassed(),
                event.totalTests(),
                event.executionTimeMs(),
                event.errorMessage(),
                testCaseResults
        );

        MistakeAnalysis analysis = mistakeClassifier.classify(problem, submission, evalResult);
        log.info("Classified mistake for submission [{}] as [{}] with confidence [{}]",
                submission.getId(), analysis.type(), analysis.confidence());

        // Record learner mistake
        Set<String> tags = problem.getTags() != null ? problem.getTags() : Set.of();
        if (analysis.type() != MistakeType.NONE) {
            mistakeTrackingService.recordMistake(submission.getUserId(), analysis.type(), tags);
        }

        // Publish AI Analysis Event
        AiAnalysisEvent aiEvent = AiAnalysisEvent.of(
                submission.getId(),
                submission.getUserId(),
                problem.getId(),
                analysis.type(),
                analysis.confidence(),
                analysis.message()
        );
        eventProducer.sendAiAnalysis(aiEvent);

        // Publish Learning Event
        LearningEvent learningEvent = LearningEvent.of(
                submission.getUserId(),
                analysis.type(),
                tags
        );
        eventProducer.sendLearningEvent(learningEvent);
    }
}
