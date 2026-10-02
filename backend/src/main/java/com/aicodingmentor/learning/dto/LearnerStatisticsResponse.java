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

        @Schema(description = "Easy problems solved count", example = "5")
        long easySolved,

        @Schema(description = "Total easy problems in system", example = "10")
        long totalEasy,

        @Schema(description = "Medium problems solved count", example = "5")
        long mediumSolved,

        @Schema(description = "Total medium problems in system", example = "15")
        long totalMedium,

        @Schema(description = "Hard problems solved count", example = "2")
        long hardSolved,

        @Schema(description = "Total hard problems in system", example = "5")
        long totalHard,

        @Schema(description = "Total problems available on platform", example = "30")
        long totalProblems,

        @Schema(description = "Total contests attended by user", example = "3")
        long contestsAttended,

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
