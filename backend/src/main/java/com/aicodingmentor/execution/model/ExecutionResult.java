package com.aicodingmentor.execution.model;

public record ExecutionResult(
        boolean success,
        String stdout,
        String stderr,
        int exitCode,
        long executionTimeMs,
        Long memoryUsageBytes,
        ExecutionFailureReason failureReason
) {
    public static ExecutionResult success(String stdout, String stderr, int exitCode, long executionTimeMs, Long memoryUsageBytes) {
        return new ExecutionResult(true, stdout, stderr, exitCode, executionTimeMs, memoryUsageBytes, null);
    }

    public static ExecutionResult compilationError(String stderr, int exitCode, long executionTimeMs) {
        return new ExecutionResult(false, "", stderr, exitCode, executionTimeMs, null, ExecutionFailureReason.COMPILATION_ERROR);
    }

    public static ExecutionResult runtimeError(String stdout, String stderr, int exitCode, long executionTimeMs) {
        return new ExecutionResult(false, stdout, stderr, exitCode, executionTimeMs, null, ExecutionFailureReason.RUNTIME_ERROR);
    }

    public static ExecutionResult timeout(long executionTimeMs) {
        return new ExecutionResult(false, "", "Execution timed out.", -1, executionTimeMs, null, ExecutionFailureReason.TIME_LIMIT_EXCEEDED);
    }

    public static ExecutionResult outputLimitExceeded(String partialStdout, long executionTimeMs) {
        return new ExecutionResult(false, partialStdout, "Output size limit exceeded.", -1, executionTimeMs, null, ExecutionFailureReason.OUTPUT_LIMIT_EXCEEDED);
    }

    public static ExecutionResult invalidSource(String reason) {
        return new ExecutionResult(false, "", reason, -1, 0, null, ExecutionFailureReason.INVALID_SOURCE);
    }

    public static ExecutionResult systemError(String message, long executionTimeMs) {
        return new ExecutionResult(false, "", message, -1, executionTimeMs, null, ExecutionFailureReason.SYSTEM_ERROR);
    }
}
