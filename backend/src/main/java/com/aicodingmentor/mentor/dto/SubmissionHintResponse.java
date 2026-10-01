package com.aicodingmentor.mentor.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Structured response containing the Socratic hint for a submission.
 */
@Schema(description = "Structured Socratic hint response for code submissions")
public record SubmissionHintResponse(
        @Schema(description = "Diagnosed mistake classification", example = "OFF_BY_ONE")
        String mistakeType,

        @Schema(description = "Delivered hint level tier (1-5)", example = "1")
        int hintLevel,

        @Schema(description = "Socratic guidance text tailored to the requested level", example = "What is the final index accessed in your array loop?")
        String hint,

        @Schema(description = "Underlying computer science or algorithmic concept", example = "0-indexed array boundaries")
        String concept,

        @Schema(description = "Indicates whether the hint reveals direct solution code (always false for levels 1-3)", example = "false")
        boolean revealsSolution
) {
}
