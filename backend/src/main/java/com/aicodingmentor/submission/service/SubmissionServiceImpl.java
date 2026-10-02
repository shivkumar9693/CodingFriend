package com.aicodingmentor.submission.service;

import com.aicodingmentor.common.api.PageResponse;
import com.aicodingmentor.common.error.ApiException;
import com.aicodingmentor.common.error.ResourceNotFoundException;
import com.aicodingmentor.common.ratelimit.RateLimiterService;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.aicodingmentor.submission.domain.Submission;
import com.aicodingmentor.submission.dto.CreateSubmissionRequest;
import com.aicodingmentor.submission.dto.SubmissionResponse;
import com.aicodingmentor.submission.dto.SubmissionSummaryResponse;
import com.aicodingmentor.submission.event.CodeSubmissionEvent;
import com.aicodingmentor.submission.exception.SubmissionAccessDeniedException;
import com.aicodingmentor.submission.exception.SubmissionNotFoundException;
import com.aicodingmentor.submission.kafka.SubmissionEventProducer;
import com.aicodingmentor.submission.repository.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

import static com.aicodingmentor.common.config.RedisCacheConfig.CACHE_SUBMISSION_STATUS;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionServiceImpl.class);

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionExecutionPlaceholderService executionPlaceholderService;
    private final SubmissionEventProducer eventProducer;
    private final RateLimiterService rateLimiterService;
    private final boolean kafkaEnabled;
    private final int submissionRateLimit;

    public SubmissionServiceImpl(
            SubmissionRepository submissionRepository,
            ProblemRepository problemRepository,
            SubmissionExecutionPlaceholderService executionPlaceholderService,
            SubmissionEventProducer eventProducer,
            RateLimiterService rateLimiterService,
            @Value("${app.kafka.enabled:false}") boolean kafkaEnabled,
            @Value("${app.ratelimit.submission-requests-per-minute:30}") int submissionRateLimit
    ) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
        this.executionPlaceholderService = executionPlaceholderService;
        this.eventProducer = eventProducer;
        this.rateLimiterService = rateLimiterService;
        this.kafkaEnabled = kafkaEnabled;
        this.submissionRateLimit = submissionRateLimit;
    }

    @Override
    @Transactional
    public SubmissionResponse createSubmission(UUID userId, CreateSubmissionRequest request) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        // Rate limit check (e.g. 30 requests/minute per user)
        if (!rateLimiterService.isAllowed("submit:" + userId, submissionRateLimit, 60)) {
            log.warn("Rate limit exceeded for user [{}] submitting code", userId);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Submission rate limit exceeded. Please wait a moment before submitting again.");
        }

        Problem problem = problemRepository.findById(request.problemId())
                .orElseThrow(() -> new ResourceNotFoundException("Problem", request.problemId()));

        Submission submission = new Submission(
                userId,
                request.problemId(),
                request.sourceCode(),
                request.language()
        );

        Submission savedSubmission = submissionRepository.save(submission);
        log.info("Created submission [{}] for user [{}] and problem [{}]",
                savedSubmission.getId(), userId, request.problemId());

        if (kafkaEnabled) {
            // Day 19 Asynchronous flow: Publish event to Kafka topic 'code-submissions'
            CodeSubmissionEvent event = CodeSubmissionEvent.of(
                    savedSubmission.getId(),
                    userId,
                    problem.getId(),
                    problem.getSlug(),
                    savedSubmission.getSourceCode(),
                    savedSubmission.getLanguage()
            );
            eventProducer.sendCodeSubmission(event);
            return SubmissionResponse.from(savedSubmission);
        } else {
            // Synchronous execution fallback for local dev / tests when Kafka is disabled
            executionPlaceholderService.enqueueForExecution(savedSubmission.getId());
            Submission evaluatedSubmission = submissionRepository.findById(savedSubmission.getId())
                    .orElse(savedSubmission);
            return SubmissionResponse.from(evaluatedSubmission);
        }
    }

    @Override
    public SubmissionResponse runCode(UUID userId, CreateSubmissionRequest request) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        Problem problem = problemRepository.findById(request.problemId())
                .orElseThrow(() -> new ResourceNotFoundException("Problem", request.problemId()));

        com.aicodingmentor.execution.model.SubmissionEvaluationResult evalResult =
                executionPlaceholderService.executePublicTestCases(
                        problem.getId(),
                        request.sourceCode(),
                        request.language()
                );

        String detailsJson = null;
        try {
            detailsJson = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(evalResult.testCaseResults());
        } catch (Exception ex) {
            log.warn("Failed to serialize test case results JSON for test run", ex);
        }

        Submission submission = new Submission(
                userId,
                request.problemId(),
                request.sourceCode(),
                request.language()
        );
        submission.markCompleted(
                evalResult.verdict(),
                evalResult.testsPassed(),
                evalResult.totalTests(),
                evalResult.totalExecutionTimeMs(),
                detailsJson,
                evalResult.compilationError()
        );

        return SubmissionResponse.from(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponse getSubmissionById(UUID submissionId, UUID requestingUserId, boolean isAdmin) {
        Objects.requireNonNull(submissionId, "submissionId must not be null");
        Objects.requireNonNull(requestingUserId, "requestingUserId must not be null");

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));

        if (!isAdmin && !submission.getUserId().equals(requestingUserId)) {
            log.warn("Unauthorized access attempt on submission [{}] by user [{}]", submissionId, requestingUserId);
            throw new SubmissionAccessDeniedException(submissionId);
        }

        return SubmissionResponse.from(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubmissionSummaryResponse> getSubmissions(
            UUID requestingUserId,
            boolean isAdmin,
            UUID filterUserId,
            UUID filterProblemId,
            Pageable pageable
    ) {
        Objects.requireNonNull(requestingUserId, "requestingUserId must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");

        Page<Submission> submissionPage;

        if (!isAdmin) {
            // Non-admin learners can only see their own submissions
            if (filterProblemId != null) {
                submissionPage = submissionRepository.findByUserIdAndProblemId(requestingUserId, filterProblemId, pageable);
            } else {
                submissionPage = submissionRepository.findByUserId(requestingUserId, pageable);
            }
        } else {
            // Administrators can view all submissions or filter by user and/or problem
            if (filterUserId != null && filterProblemId != null) {
                submissionPage = submissionRepository.findByUserIdAndProblemId(filterUserId, filterProblemId, pageable);
            } else if (filterUserId != null) {
                submissionPage = submissionRepository.findByUserId(filterUserId, pageable);
            } else if (filterProblemId != null) {
                submissionPage = submissionRepository.findByProblemId(filterProblemId, pageable);
            } else {
                submissionPage = submissionRepository.findAll(pageable);
            }
        }

        Page<SubmissionSummaryResponse> summaryPage = submissionPage.map(SubmissionSummaryResponse::from);
        return PageResponse.from(summaryPage);
    }
}
