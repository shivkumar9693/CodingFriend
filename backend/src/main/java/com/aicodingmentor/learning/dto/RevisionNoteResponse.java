package com.aicodingmentor.learning.dto;

import com.aicodingmentor.learning.domain.MasteryStatus;
import com.aicodingmentor.learning.domain.MistakeCategory;
import com.aicodingmentor.learning.domain.RevisionNote;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Detailed response for a saved revision note")
public record RevisionNoteResponse(
        @Schema(description = "Unique note ID")
        UUID id,

        @Schema(description = "User ID")
        UUID userId,

        @Schema(description = "Problem ID")
        UUID problemId,

        @Schema(description = "Submission ID")
        UUID submissionId,

        @Schema(description = "Problem title")
        String problemTitle,

        @Schema(description = "Problem slug")
        String problemSlug,

        @Schema(description = "Language")
        String language,

        @Schema(description = "Mistake Category")
        MistakeCategory mistakeCategory,

        @Schema(description = "Concept summary")
        String conceptSummary,

        @Schema(description = "Failed input")
        String failedInput,

        @Schema(description = "Actual output")
        String actualOutput,

        @Schema(description = "Expected output")
        String expectedOutput,

        @Schema(description = "Student execution flow trace")
        String studentFlowTrace,

        @Schema(description = "Correct invariant flow trace")
        String correctFlowTrace,

        @Schema(description = "Key takeaway / mental model")
        String keyTakeaway,

        @Schema(description = "Submitted source code")
        String submittedCode,

        @Schema(description = "User reflections")
        String userReflections,

        @Schema(description = "Mastery status")
        MasteryStatus masteryStatus,

        @Schema(description = "Whether bookmarked")
        boolean isBookmarked,

        @Schema(description = "Spaced repetition review count")
        int reviewCount,

        @Schema(description = "Last reviewed timestamp")
        Instant lastReviewedAt,

        @Schema(description = "Next scheduled review timestamp")
        Instant nextReviewAt,

        @Schema(description = "Creation timestamp")
        Instant createdAt,

        @Schema(description = "Update timestamp")
        Instant updatedAt
) {
    public static RevisionNoteResponse from(RevisionNote entity) {
        return new RevisionNoteResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getProblemId(),
                entity.getSubmissionId(),
                entity.getProblemTitle(),
                entity.getProblemSlug(),
                entity.getLanguage(),
                entity.getMistakeCategory(),
                entity.getConceptSummary(),
                entity.getFailedInput(),
                entity.getActualOutput(),
                entity.getExpectedOutput(),
                entity.getStudentFlowTrace(),
                entity.getCorrectFlowTrace(),
                entity.getKeyTakeaway(),
                entity.getSubmittedCode(),
                entity.getUserReflections(),
                entity.getMasteryStatus(),
                entity.isBookmarked(),
                entity.getReviewCount(),
                entity.getLastReviewedAt(),
                entity.getNextReviewAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
