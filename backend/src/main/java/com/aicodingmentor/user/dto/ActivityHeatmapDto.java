package com.aicodingmentor.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "365-day user activity heatmap and streak telemetry")
public record ActivityHeatmapDto(
        @Schema(description = "Current active problem solving streak in days", example = "5")
        int currentStreak,

        @Schema(description = "Maximum historic problem solving streak in days", example = "14")
        int maxStreak,

        @Schema(description = "Total number of active coding days in the past 365 days", example = "42")
        int totalActiveDays,

        @Schema(description = "Total submissions made across the past 365 days", example = "156")
        int totalSubmissions,

        @Schema(description = "Total unique problems solved in the past 365 days", example = "38")
        int totalProblemsSolved,

        @Schema(description = "Daily activity records for rendering the 365-day heatmap grid")
        List<DayActivityDto> dailyActivities
) {
    public record DayActivityDto(
            LocalDate date,
            int count,
            int solvedCount
    ) {}
}
