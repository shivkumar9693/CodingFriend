package com.aicodingmentor.submission.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published to Kafka topic 'code-submissions' upon persistence of a learner submission.
 */
public record CodeSubmissionEvent(
        UUID submissionId,
        UUID userId,
        UUID problemId,
        String problemSlug,
        String sourceCode,
        String language,
        Instant timestamp
) {
    public static CodeSubmissionEvent of(UUID submissionId, UUID userId, UUID problemId, String problemSlug, String sourceCode, String language) {
        return new CodeSubmissionEvent(submissionId, userId, problemId, problemSlug, sourceCode, language, Instant.now());
    }
}
