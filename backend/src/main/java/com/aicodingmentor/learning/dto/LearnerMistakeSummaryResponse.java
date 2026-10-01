package com.aicodingmentor.learning.dto;

import com.aicodingmentor.learning.domain.LearnerMistakeRecord;
import com.aicodingmentor.mistake.model.MistakeType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Summary DTO representing a learner's recurring mistake pattern.
 */
@Schema(description = "Summary of a learner's recurring mistake pattern")
public record LearnerMistakeSummaryResponse(
        @Schema(description = "Mistake record UUID")
        UUID id,

        @Schema(description = "Mistake classification type", example = "OFF_BY_ONE")
        MistakeType mistakeType,

        @Schema(description = "Total occurrences count", example = "4")
        int frequency,

        @Schema(description = "First occurrence timestamp")
        Instant firstOccurrence,

        @Schema(description = "Last occurrence timestamp")
        Instant lastOccurrence,

        @Schema(description = "Related topic tags (e.g. ARRAY, BINARY_SEARCH)")
        Set<String> relatedTopics
) {
    public static LearnerMistakeSummaryResponse from(LearnerMistakeRecord record) {
        return new LearnerMistakeSummaryResponse(
                record.getId(),
                record.getMistakeType(),
                record.getFrequency(),
                record.getFirstOccurrence(),
                record.getLastOccurrence(),
                record.getRelatedTopics()
        );
    }
}
