package com.aicodingmentor.execution.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Evaluation result for an individual test case, masking hidden test details")
public record TestCaseEvaluationResult(
        @Schema(description = "1-indexed test case number", example = "1")
        int testNumber,

        @Schema(description = "Whether this is a secret/hidden grading test case", example = "false")
        boolean isHidden,

        @Schema(description = "Whether the test case passed", example = "true")
        boolean passed,

        @Schema(description = "Verdict for this specific test case", example = "ACCEPTED")
        EvaluationVerdict verdict,

        @Schema(description = "Input data (null if hidden)", example = "2 3\\n")
        String input,

        @Schema(description = "Expected output (null if hidden)", example = "5\\n")
        String expectedOutput,

        @Schema(description = "Actual sanitized standard output produced (null if hidden)", example = "5\\n")
        String actualOutput,

        @Schema(description = "Execution duration in milliseconds", example = "42")
        long executionTimeMs,

        @Schema(description = "Error or diagnostic message if test failed", example = "null")
        String errorMessage
) {
    public static TestCaseEvaluationResult passed(
            int testNumber,
            boolean isHidden,
            String input,
            String expectedOutput,
            String actualOutput,
            long executionTimeMs
    ) {
        if (isHidden) {
            return new TestCaseEvaluationResult(
                    testNumber,
                    true,
                    true,
                    EvaluationVerdict.ACCEPTED,
                    null,
                    null,
                    null,
                    executionTimeMs,
                    null
            );
        }
        return new TestCaseEvaluationResult(
                testNumber,
                false,
                true,
                EvaluationVerdict.ACCEPTED,
                input,
                expectedOutput,
                actualOutput,
                executionTimeMs,
                null
        );
    }

    public static TestCaseEvaluationResult failed(
            int testNumber,
            boolean isHidden,
            EvaluationVerdict verdict,
            String input,
            String expectedOutput,
            String actualOutput,
            long executionTimeMs,
            String errorMessage
    ) {
        if (isHidden) {
            return new TestCaseEvaluationResult(
                    testNumber,
                    true,
                    false,
                    verdict,
                    null,
                    null,
                    null,
                    executionTimeMs,
                    errorMessage != null ? sanitizeHiddenErrorMessage(errorMessage) : null
            );
        }
        return new TestCaseEvaluationResult(
                testNumber,
                false,
                false,
                verdict,
                input,
                expectedOutput,
                actualOutput,
                executionTimeMs,
                errorMessage
        );
    }

    private static String sanitizeHiddenErrorMessage(String error) {
        if (error.contains("ArithmeticException")) {
            return "ArithmeticException during execution";
        }
        if (error.contains("NullPointerException")) {
            return "NullPointerException during execution";
        }
        if (error.contains("ArrayIndexOutOfBoundsException") || error.contains("IndexOutOfBoundsException")) {
            return "IndexOutOfBoundsException during execution";
        }
        if (error.contains("StackOverflowError")) {
            return "StackOverflowError (potential infinite recursion)";
        }
        return "Runtime exception encountered on hidden test case";
    }
}
