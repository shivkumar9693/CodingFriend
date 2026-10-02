package com.aicodingmentor.learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Summary metrics for the learner's revision notebook")
public record RevisionNotesSummaryDto(
        @Schema(description = "Total number of notes saved")
        long totalNotes,

        @Schema(description = "Notes currently in 'TO_REVISE' status")
        long toReviseCount,

        @Schema(description = "Notes that have been reviewed")
        long revisedCount,

        @Schema(description = "Notes that have been mastered")
        long masteredCount,

        @Schema(description = "Bookmarked notes count")
        long bookmarkedCount,

        @Schema(description = "Notes due for spaced repetition review today")
        long dueTodayCount,

        @Schema(description = "Distribution of mistakes across categories")
        Map<String, Long> categoryBreakdown
) {
}
