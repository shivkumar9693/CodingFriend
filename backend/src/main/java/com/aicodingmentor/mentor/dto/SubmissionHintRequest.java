package com.aicodingmentor.mentor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request payload for requesting tiered AI mentor hints on a specific submission.
 */
@Schema(description = "Request body for submission-level AI mentor hints")
public record SubmissionHintRequest(
        @Min(value = 1, message = "Hint level must be between 1 and 5")
        @Max(value = 5, message = "Hint level must be between 1 and 5")
        @Schema(description = "Requested Socratic hint tier (1=guiding question, 2=conceptual hint, 3=targeted example, 4=detailed explanation, 5=solution guidance)", example = "1")
        int level
) {
}
