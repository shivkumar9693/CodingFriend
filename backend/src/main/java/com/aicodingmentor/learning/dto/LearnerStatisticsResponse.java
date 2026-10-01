package com.aicodingmentor.learning.dto;

import com.aicodingmentor.mistake.model.MistakeType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Learner aggregate coding & learning statistics.
 */
@Schema(description = "Aggregated learner performance and mistake statistics")
public record LearnerStatisticsResponse(
        @Schema(description = "Total submissions made by the learner", example = "42")
        long totalSubmissions,

        @Schema(description = "Total successfully accepted submissions", example = "28")
        long totalAccepted,

        @Schema(description = "Overall success rate percentage (0.0 - 100.0)", example = "66.67")
        double accuracyPercentage,

        @Schema(description = "Total distinct problems solved", example = "12")
        long problemsSolved,

        @Schema(description = "Total distinct problems attempted", example = "15")
        long problemsAttempted,

        @Schema(description = "Average attempts per problem attempted", example = "2.8")
        double averageAttemptsPerProblem,

        @Schema(description = "Most frequent mistake category", example = "OFF_BY_ONE")
        MistakeType mostFrequentMistake,

        @Schema(description = "Mistake frequency breakdown by category")
        Map<MistakeType, Integer> mistakeBreakdown,

        @Schema(description = "Weak topic areas based on mistake tracking")
        Map<String, Integer> weakTopics
) {
}
