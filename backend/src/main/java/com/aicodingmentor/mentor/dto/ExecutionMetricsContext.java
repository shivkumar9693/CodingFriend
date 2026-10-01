package com.aicodingmentor.mentor.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Execution metrics context for AI Socratic prompts.
 */
@Schema(description = "Execution performance metrics for AI contextual reasoning")
public record ExecutionMetricsContext(
        @Schema(description = "Wall-clock execution time in milliseconds", example = "42")
        Long executionTimeMs,

        @Schema(description = "Peak memory usage in bytes", example = "16777216")
        Long memoryUsageBytes,

        @Schema(description = "Process exit code", example = "0")
        Integer exitCode
) {
    public static ExecutionMetricsContext of(Long executionTimeMs, Long memoryUsageBytes, Integer exitCode) {
        return new ExecutionMetricsContext(executionTimeMs, memoryUsageBytes, exitCode);
    }
}
