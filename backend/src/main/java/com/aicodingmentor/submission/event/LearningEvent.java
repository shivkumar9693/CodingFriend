package com.aicodingmentor.submission.event;

import com.aicodingmentor.mistake.model.MistakeType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Event published to Kafka topic 'learning-events' when learner mistake patterns update.
 */
public record LearningEvent(
        UUID userId,
        MistakeType mistakeType,
        Set<String> problemTags,
        Instant timestamp
) {
    public static LearningEvent of(UUID userId, MistakeType mistakeType, Set<String> problemTags) {
        return new LearningEvent(userId, mistakeType, problemTags, Instant.now());
    }
}
