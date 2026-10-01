package com.aicodingmentor.submission.event;

import com.aicodingmentor.execution.model.EvaluationVerdict;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published to Kafka topic 'execution-results' after code execution in isolated sandbox.
 */
public record ExecutionResultEvent(
        UUID submissionId,
        UUID userId,
        UUID problemId,
        EvaluationVerdict verdict,
        int testsPassed,
        int totalTests,
        long executionTimeMs,
        String errorMessage,
        Instant timestamp
) {
    public static ExecutionResultEvent of(
            UUID submissionId,
            UUID userId,
            UUID problemId,
            EvaluationVerdict verdict,
            int testsPassed,
            int totalTests,
            long executionTimeMs,
            String errorMessage
    ) {
        return new ExecutionResultEvent(
                submissionId,
                userId,
                problemId,
                verdict,
                testsPassed,
                totalTests,
                executionTimeMs,
                errorMessage,
                Instant.now()
        );
    }
}
