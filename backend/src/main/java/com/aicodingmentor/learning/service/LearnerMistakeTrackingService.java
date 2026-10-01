package com.aicodingmentor.learning.service;

import com.aicodingmentor.learning.dto.LearnerMistakeSummaryResponse;
import com.aicodingmentor.learning.dto.LearnerStatisticsResponse;
import com.aicodingmentor.mistake.model.MistakeType;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service contract for tracking learner recurring mistake patterns and aggregate analytics.
 */
public interface LearnerMistakeTrackingService {

    /**
     * Records an occurrence of a mistake for a user.
     *
     * @param userId user identifier
     * @param mistakeType classification of mistake
     * @param relatedTopics related problem tag topics
     */
    void recordMistake(UUID userId, MistakeType mistakeType, Set<String> relatedTopics);

    /**
     * Retrieves the chronological and frequency summary of mistakes for a user.
     *
     * @param userId user identifier
     * @return list of mistake summary responses
     */
    List<LearnerMistakeSummaryResponse> getLearnerMistakes(UUID userId);

    /**
     * Computes aggregate learner performance and mistake distribution analytics.
     *
     * @param userId user identifier
     * @return structured statistics response
     */
    LearnerStatisticsResponse getLearnerStatistics(UUID userId);
}
