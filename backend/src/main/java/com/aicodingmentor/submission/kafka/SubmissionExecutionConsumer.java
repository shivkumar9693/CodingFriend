package com.aicodingmentor.submission.kafka;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.execution.model.SubmissionEvaluationResult;
import com.aicodingmentor.execution.service.TestCaseEvaluator;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.TestCase;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.aicodingmentor.problem.service.TestCaseService;
import com.aicodingmentor.submission.domain.Submission;
import com.aicodingmentor.submission.domain.SubmissionStatus;
import com.aicodingmentor.submission.event.CodeSubmissionEvent;
import com.aicodingmentor.submission.event.ExecutionResultEvent;
import com.aicodingmentor.submission.repository.SubmissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.aicodingmentor.common.config.RedisCacheConfig.CACHE_SUBMISSION_STATUS;
import static com.aicodingmentor.submission.kafka.KafkaTopicConfig.TOPIC_CODE_SUBMISSIONS;

/**
 * Consumer for 'code-submissions' topic.
 * Enforces idempotency to avoid re-evaluating duplicate redeliveries and dispatches
 * sandboxed test execution before publishing results to 'execution-results'.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class SubmissionExecutionConsumer {

    private static final Logger log = LoggerFactory.getLogger(SubmissionExecutionConsumer.class);

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final TestCaseService testCaseService;
    private final TestCaseEvaluator testCaseEvaluator;
    private final SubmissionEventProducer eventProducer;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;
    private final com.aicodingmentor.user.service.UserService userService;

    public SubmissionExecutionConsumer(
            SubmissionRepository submissionRepository,
            ProblemRepository problemRepository,
            TestCaseService testCaseService,
            TestCaseEvaluator testCaseEvaluator,
            SubmissionEventProducer eventProducer,
            ObjectMapper objectMapper,
            CacheManager cacheManager,
            com.aicodingmentor.user.service.UserService userService
    ) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
        this.testCaseService = testCaseService;
        this.testCaseEvaluator = testCaseEvaluator;
        this.eventProducer = eventProducer;
        this.objectMapper = objectMapper;
        this.cacheManager = cacheManager;
        this.userService = userService;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = TOPIC_CODE_SUBMISSIONS, groupId = "submission-execution-group")
    @Transactional
    public void consumeSubmission(CodeSubmissionEvent event) {
        if (event == null || event.submissionId() == null) {
            log.warn("Received empty or null CodeSubmissionEvent");
            return;
        }

        log.info("Processing CodeSubmissionEvent from Kafka for submission [{}]", event.submissionId());

        Submission submission = submissionRepository.findById(event.submissionId()).orElse(null);
        if (submission == null) {
            log.warn("Submission [{}] not found in database. Skipping execution.", event.submissionId());
            return;
        }

        // Idempotency check: Ignore duplicate deliveries if submission has already been evaluated
        if (submission.getStatus() == SubmissionStatus.COMPLETED) {
            log.info("Idempotency guard: Submission [{}] is already COMPLETED with verdict [{}]. Skipping duplicate execution.",
                    submission.getId(), submission.getVerdict());
            return;
        }

        submission.markEvaluating();
        submissionRepository.save(submission);

        List<TestCase> testCases = testCaseService.getTestCasesForExecution(submission.getProblemId());
        String problemSlug = event.problemSlug() != null ? event.problemSlug() :
                problemRepository.findById(submission.getProblemId()).map(Problem::getSlug).orElse(null);

        SubmissionEvaluationResult evalResult = testCaseEvaluator.evaluate(
                submission.getSourceCode(),
                submission.getLanguage(),
                testCases,
                5000L,
                256,
                problemSlug
        );

        String detailsJson = null;
        try {
            detailsJson = objectMapper.writeValueAsString(evalResult.testCaseResults());
        } catch (Exception ex) {
            log.warn("Failed to serialize test case details for submission [{}]", submission.getId(), ex);
        }

        String firstErrorMessage = evalResult.compilationError();
        if (firstErrorMessage == null && evalResult.testCaseResults() != null) {
            firstErrorMessage = evalResult.testCaseResults().stream()
                    .filter(tc -> !tc.passed() && tc.errorMessage() != null && !tc.errorMessage().isBlank())
                    .map(tc -> tc.errorMessage())
                    .findFirst()
                    .orElse(null);
        }

        submission.markCompleted(
                evalResult.verdict(),
                evalResult.testsPassed(),
                evalResult.totalTests(),
                evalResult.totalExecutionTimeMs(),
                detailsJson
        );
        submission.setErrorMessage(firstErrorMessage);

        Submission saved = submissionRepository.save(submission);
        log.info("Successfully executed submission [{}] with verdict [{}] (passed: {}/{})",
                saved.getId(), saved.getVerdict(), saved.getTestsPassed(), saved.getTotalTests());

        // Invalidate or update submission-status cache
        try {
            var cache = cacheManager.getCache(CACHE_SUBMISSION_STATUS);
            if (cache != null) {
                cache.evict(saved.getId());
            }
        } catch (Exception ex) {
            log.warn("Failed to evict submission-status cache for [{}]: {}", saved.getId(), ex.getMessage());
        }

        // Record user daily streak and activity
        if (userService != null && saved.getUserId() != null) {
            try {
                userService.recordSubmissionActivity(saved.getUserId(), saved.getVerdict() == EvaluationVerdict.ACCEPTED);
            } catch (Exception ex) {
                log.warn("Failed to record submission activity for user [{}]: {}", saved.getUserId(), ex.getMessage());
            }
        }

        // Publish to execution-results topic
        ExecutionResultEvent resultEvent = ExecutionResultEvent.of(
                saved.getId(),
                saved.getUserId(),
                saved.getProblemId(),
                saved.getVerdict(),
                saved.getTestsPassed(),
                saved.getTotalTests(),
                saved.getExecutionTimeMs(),
                saved.getErrorMessage()
        );
        eventProducer.sendExecutionResult(resultEvent);
    }
}
