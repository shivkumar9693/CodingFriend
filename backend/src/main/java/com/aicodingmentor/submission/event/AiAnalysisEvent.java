package com.aicodingmentor.submission.event;

import com.aicodingmentor.mistake.model.MistakeType;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published to Kafka topic 'ai-analysis' upon AI mistake classification.
 */
public record AiAnalysisEvent(
        UUID submissionId,
        UUID userId,
        UUID problemId,
        MistakeType mistakeType,
        double confidence,
        String explanation,
        Instant timestamp
) {
    public static AiAnalysisEvent of(UUID submissionId, UUID userId, UUID problemId, MistakeType mistakeType, double confidence, String explanation) {
        return new AiAnalysisEvent(submissionId, userId, problemId, mistakeType, confidence, explanation, Instant.now());
    }
}
