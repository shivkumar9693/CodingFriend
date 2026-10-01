package com.aicodingmentor.execution.service;

import com.aicodingmentor.execution.model.SubmissionEvaluationResult;
import com.aicodingmentor.problem.domain.TestCase;

import java.util.List;

public interface TestCaseEvaluator {

    /**
     * Evaluates source code against public and hidden test suites.
     *
     * @param sourceCode the learner's submitted source code
     * @param language programming language (default JAVA)
     * @param testCases list of test cases (both public and hidden)
     * @param timeoutMs execution timeout per test case in milliseconds
     * @param memoryLimitMb memory limit in megabytes
     * @return structured submission evaluation result
     */
    SubmissionEvaluationResult evaluate(
            String sourceCode,
            String language,
            List<TestCase> testCases,
            long timeoutMs,
            int memoryLimitMb
    );

    default SubmissionEvaluationResult evaluate(
            String sourceCode,
            String language,
            List<TestCase> testCases,
            long timeoutMs,
            int memoryLimitMb,
            String problemSlug
    ) {
        return evaluate(sourceCode, language, testCases, timeoutMs, memoryLimitMb);
    }
}
