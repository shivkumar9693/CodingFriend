package com.aicodingmentor.learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

@Schema(description = "Payload to save an AI Post-Mortem counter-example into learner's revision notebook")
public record CreateRevisionNoteRequest(
        @Schema(description = "Problem ID")
        UUID problemId,

        @Schema(description = "Submission ID")
        UUID submissionId,

        @NotBlank(message = "Problem title is required")
        @Schema(description = "Problem Title", example = "Two Sum")
        String problemTitle,

        @Schema(description = "Problem slug", example = "two-sum")
        String problemSlug,

        @Schema(description = "Language", example = "JAVA")
        String language,

        @NotBlank(message = "Mistake category is required")
        @Schema(description = "Mistake Category", example = "OFF_BY_ONE")
        String mistakeCategory,

        @NotBlank(message = "Concept summary is required")
        @Schema(description = "Concept Summary")
        String conceptSummary,

        @Schema(description = "Failed Input counter-example")
        String failedInput,

        @Schema(description = "Actual Output")
        String actualOutput,

        @Schema(description = "Expected Output")
        String expectedOutput,

        @NotBlank(message = "Student flow trace is required")
        @Schema(description = "Actual flow executed by student code")
        String studentFlowTrace,

        @NotBlank(message = "Correct flow trace is required")
        @Schema(description = "Correct invariant flow trace")
        String correctFlowTrace,

        @NotBlank(message = "Key takeaway is required")
        @Schema(description = "Key takeaway / mental model")
        String keyTakeaway,

        @Schema(description = "Submitted source code")
        String submittedCode,

        @Schema(description = "Learner's personal reflection or notes")
        String userReflections,

        @Schema(description = "Whether note is bookmarked")
        Boolean isBookmarked
) {
}
