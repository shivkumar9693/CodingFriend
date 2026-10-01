package com.aicodingmentor.submission.service;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.execution.model.SubmissionEvaluationResult;
import com.aicodingmentor.execution.service.TestCaseEvaluator;
import com.aicodingmentor.problem.domain.TestCase;
import com.aicodingmentor.problem.service.TestCaseService;
import com.aicodingmentor.submission.domain.Submission;
import com.aicodingmentor.submission.repository.SubmissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SubmissionExecutionPlaceholderService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionExecutionPlaceholderService.class);

    private final SubmissionRepository submissionRepository;
    private final com.aicodingmentor.problem.repository.ProblemRepository problemRepository;
    private final TestCaseService testCaseService;
    private final TestCaseEvaluator testCaseEvaluator;
    private final ObjectMapper objectMapper;

    public SubmissionExecutionPlaceholderService(
            SubmissionRepository submissionRepository,
            com.aicodingmentor.problem.repository.ProblemRepository problemRepository,
            TestCaseService testCaseService,
            TestCaseEvaluator testCaseEvaluator,
            ObjectMapper objectMapper
    ) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
        this.testCaseService = testCaseService;
        this.testCaseEvaluator = testCaseEvaluator;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes test case evaluation synchronously for local development without Kafka.
     */
    @Transactional
    public void enqueueForExecution(UUID submissionId) {
        if (submissionId == null) {
            return;
        }

        Submission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null) {
            log.warn("Cannot evaluate submission [{}]: not found", submissionId);
            return;
        }

        try {
            submission.markEvaluating();
            submissionRepository.save(submission);

            List<TestCase> testCases = testCaseService.getTestCasesForExecution(submission.getProblemId());
            String problemSlug = problemRepository.findById(submission.getProblemId())
                    .map(com.aicodingmentor.problem.domain.Problem::getSlug)
                    .orElse(null);

            log.info("Evaluating submission [{}] for problem [{}] against {} test cases (lang: {})",
                    submissionId, problemSlug, testCases.size(), submission.getLanguage());
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
                log.warn("Failed to serialize test case results JSON for submission [{}]", submissionId, ex);
            }

            if (evalResult.verdict() == EvaluationVerdict.COMPILE_ERROR) {
                submission.markCompleted(
                        EvaluationVerdict.COMPILE_ERROR,
                        0,
                        evalResult.totalTests(),
                        evalResult.totalExecutionTimeMs(),
                        detailsJson
                );
                submission.setErrorMessage(evalResult.compilationError());
            } else {
                submission.markCompleted(
                        evalResult.verdict(),
                        evalResult.testsPassed(),
                        evalResult.totalTests(),
                        evalResult.totalExecutionTimeMs(),
                        detailsJson
                );
            }

            submissionRepository.save(submission);
            log.info("Submission [{}] evaluated with verdict [{}] ({}/{} passed, {}ms)",
                    submissionId, evalResult.verdict(), evalResult.testsPassed(), evalResult.totalTests(), evalResult.totalExecutionTimeMs());

        } catch (Exception ex) {
            log.error("Failed to evaluate submission [{}]", submissionId, ex);
            submission.markFailed("Evaluation engine error: " + ex.getMessage());
            submissionRepository.save(submission);
        }
    }
}
