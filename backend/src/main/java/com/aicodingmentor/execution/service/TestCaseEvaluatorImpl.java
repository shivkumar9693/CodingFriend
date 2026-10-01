package com.aicodingmentor.execution.service;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.execution.model.ExecutionFailureReason;
import com.aicodingmentor.execution.model.ExecutionRequest;
import com.aicodingmentor.execution.model.ExecutionResult;
import com.aicodingmentor.execution.model.SubmissionEvaluationResult;
import com.aicodingmentor.execution.model.SupportedLanguage;
import com.aicodingmentor.execution.model.TestCaseEvaluationResult;
import com.aicodingmentor.problem.domain.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TestCaseEvaluatorImpl implements TestCaseEvaluator {

    private static final Logger log = LoggerFactory.getLogger(TestCaseEvaluatorImpl.class);

    private final CodeExecutor codeExecutor;
    private final OutputComparator outputComparator;
    private final DriverHarnessInjector driverHarnessInjector;

    public TestCaseEvaluatorImpl(
            CodeExecutor codeExecutor,
            OutputComparator outputComparator,
            DriverHarnessInjector driverHarnessInjector
    ) {
        this.codeExecutor = codeExecutor;
        this.outputComparator = outputComparator;
        this.driverHarnessInjector = driverHarnessInjector;
    }

    @Override
    public SubmissionEvaluationResult evaluate(
            String sourceCode,
            String language,
            List<TestCase> testCases,
            long timeoutMs,
            int memoryLimitMb
    ) {
        return evaluate(sourceCode, language, testCases, timeoutMs, memoryLimitMb, null);
    }

    @Override
    public SubmissionEvaluationResult evaluate(
            String sourceCode,
            String language,
            List<TestCase> testCases,
            long timeoutMs,
            int memoryLimitMb,
            String explicitProblemSlug
    ) {
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return SubmissionEvaluationResult.compileError("Source code must not be null or empty.", 0);
        }

        if (testCases == null || testCases.isEmpty()) {
            return SubmissionEvaluationResult.of(EvaluationVerdict.ACCEPTED, 0, 0, 0, List.of());
        }

        // Sort test cases by sequence order
        List<TestCase> sortedCases = testCases.stream()
                .sorted(Comparator.comparingInt(TestCase::getSequenceOrder))
                .toList();

        // Resolve problem slug if available
        String problemSlug = explicitProblemSlug;
        if (problemSlug == null || problemSlug.isBlank()) {
            try {
                if (!sortedCases.isEmpty() && sortedCases.get(0).getProblem() != null) {
                    problemSlug = sortedCases.get(0).getProblem().getSlug();
                }
            } catch (Exception ignored) {
            }
        }

        SupportedLanguage targetLang = SupportedLanguage.fromString(language);
        String executableCode = driverHarnessInjector.wrapUserCode(sourceCode, targetLang, problemSlug);

        List<TestCaseEvaluationResult> evaluationResults = new ArrayList<>(sortedCases.size());
        int testsPassed = 0;
        long totalExecutionTimeMs = 0;
        EvaluationVerdict overallVerdict = EvaluationVerdict.ACCEPTED;

        for (int i = 0; i < sortedCases.size(); i++) {
            TestCase tc = sortedCases.get(i);
            int testNumber = i + 1;
            boolean isHidden = tc.isHidden();

            ExecutionRequest request = new ExecutionRequest(
                    executableCode,
                    null,
                    tc.getInputData() != null ? tc.getInputData() : "",
                    timeoutMs > 0 ? timeoutMs : ExecutionRequest.DEFAULT_TIMEOUT_MS,
                    memoryLimitMb > 0 ? memoryLimitMb : ExecutionRequest.DEFAULT_MEMORY_LIMIT_MB,
                    ExecutionRequest.DEFAULT_CPU_QUOTA,
                    ExecutionRequest.DEFAULT_MAX_OUTPUT_SIZE_BYTES,
                    targetLang
            );

            ExecutionResult result = codeExecutor.execute(request);
            totalExecutionTimeMs += result.executionTimeMs();

            // 1. Compilation Error aborts immediately
            if (result.failureReason() == ExecutionFailureReason.COMPILATION_ERROR) {
                log.info("Compilation failed during test case evaluation for language [{}]: {}", targetLang, result.stderr());
                return SubmissionEvaluationResult.compileError(result.stderr(), totalExecutionTimeMs);
            }

            // 2. Timeout
            if (result.failureReason() == ExecutionFailureReason.TIME_LIMIT_EXCEEDED) {
                TestCaseEvaluationResult tcResult = TestCaseEvaluationResult.failed(
                        testNumber,
                        isHidden,
                        EvaluationVerdict.TIME_LIMIT_EXCEEDED,
                        tc.getInputData(),
                        tc.getExpectedOutput(),
                        "",
                        result.executionTimeMs(),
                        "Time limit exceeded (" + request.timeoutMs() + "ms)"
                );
                evaluationResults.add(tcResult);
                if (overallVerdict == EvaluationVerdict.ACCEPTED) {
                    overallVerdict = EvaluationVerdict.TIME_LIMIT_EXCEEDED;
                }
                continue;
            }

            // 3. Memory Limit
            if (result.failureReason() == ExecutionFailureReason.MEMORY_LIMIT_EXCEEDED) {
                TestCaseEvaluationResult tcResult = TestCaseEvaluationResult.failed(
                        testNumber,
                        isHidden,
                        EvaluationVerdict.MEMORY_LIMIT_EXCEEDED,
                        tc.getInputData(),
                        tc.getExpectedOutput(),
                        "",
                        result.executionTimeMs(),
                        "Memory limit exceeded"
                );
                evaluationResults.add(tcResult);
                if (overallVerdict == EvaluationVerdict.ACCEPTED) {
                    overallVerdict = EvaluationVerdict.MEMORY_LIMIT_EXCEEDED;
                }
                continue;
            }

            // 4. Runtime Error
            if (result.failureReason() == ExecutionFailureReason.RUNTIME_ERROR || !result.success()) {
                TestCaseEvaluationResult tcResult = TestCaseEvaluationResult.failed(
                        testNumber,
                        isHidden,
                        EvaluationVerdict.RUNTIME_ERROR,
                        tc.getInputData(),
                        tc.getExpectedOutput(),
                        result.stdout(),
                        result.executionTimeMs(),
                        result.stderr() != null && !result.stderr().isEmpty()
                                ? result.stderr()
                                : "Process exited with non-zero code " + result.exitCode()
                );
                evaluationResults.add(tcResult);
                if (overallVerdict == EvaluationVerdict.ACCEPTED) {
                    overallVerdict = EvaluationVerdict.RUNTIME_ERROR;
                }
                continue;
            }

            // 5. Compare Output
            boolean matched = outputComparator.matches(tc.getExpectedOutput(), result.stdout());
            if (matched) {
                testsPassed++;
                TestCaseEvaluationResult tcResult = TestCaseEvaluationResult.passed(
                        testNumber,
                        isHidden,
                        tc.getInputData(),
                        tc.getExpectedOutput(),
                        result.stdout(),
                        result.executionTimeMs()
                );
                evaluationResults.add(tcResult);
            } else {
                TestCaseEvaluationResult tcResult = TestCaseEvaluationResult.failed(
                        testNumber,
                        isHidden,
                        EvaluationVerdict.WRONG_ANSWER,
                        tc.getInputData(),
                        tc.getExpectedOutput(),
                        result.stdout(),
                        result.executionTimeMs(),
                        "Output mismatch"
                );
                evaluationResults.add(tcResult);
                if (overallVerdict == EvaluationVerdict.ACCEPTED) {
                    overallVerdict = EvaluationVerdict.WRONG_ANSWER;
                }
            }
        }

        if (testsPassed == sortedCases.size()) {
            overallVerdict = EvaluationVerdict.ACCEPTED;
        }

        return SubmissionEvaluationResult.of(
                overallVerdict,
                sortedCases.size(),
                testsPassed,
                totalExecutionTimeMs,
                evaluationResults
        );
    }
}
