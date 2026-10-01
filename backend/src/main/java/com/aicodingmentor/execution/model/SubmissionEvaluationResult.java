package com.aicodingmentor.execution.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Aggregated evaluation result across all problem test cases")
public record SubmissionEvaluationResult(
        @Schema(description = "Overall evaluation verdict", example = "ACCEPTED")
        EvaluationVerdict verdict,

        @Schema(description = "Total number of test cases evaluated", example = "5")
        int totalTests,

        @Schema(description = "Number of test cases passed", example = "5")
        int testsPassed,

        @Schema(description = "Total execution time in milliseconds", example = "120")
        long totalExecutionTimeMs,

        @Schema(description = "Compilation error details if compilation failed", example = "null")
        String compilationError,

        @Schema(description = "List of individual test case evaluation outcomes")
        List<TestCaseEvaluationResult> testCaseResults
) {
    public static SubmissionEvaluationResult compileError(String errorMessage, long executionTimeMs) {
        return new SubmissionEvaluationResult(
                EvaluationVerdict.COMPILE_ERROR,
                0,
                0,
                executionTimeMs,
                errorMessage,
                List.of()
        );
    }

    public static SubmissionEvaluationResult of(
            EvaluationVerdict verdict,
            int totalTests,
            int testsPassed,
            long totalExecutionTimeMs,
            List<TestCaseEvaluationResult> testCaseResults
    ) {
        return new SubmissionEvaluationResult(
                verdict,
                totalTests,
                testsPassed,
                totalExecutionTimeMs,
                null,
                testCaseResults != null ? testCaseResults : List.of()
        );
    }
}
