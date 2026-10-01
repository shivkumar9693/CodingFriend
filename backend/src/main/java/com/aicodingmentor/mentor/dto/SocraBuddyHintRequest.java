package com.aicodingmentor.mentor.dto;

import com.aicodingmentor.mentor.model.HintLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request body for asking SocraBuddy for a tiered Socratic hint")
public record SocraBuddyHintRequest(
        @NotNull(message = "Problem ID must not be null")
        @Schema(description = "Target problem ID", example = "8e177368-aba4-43ce-bb1b-32ee2ffa1f80")
        UUID problemId,

        @Schema(description = "Optional submission ID if requesting hint for a specific submission attempt")
        UUID submissionId,

        @NotBlank(message = "Source code must not be blank")
        @Schema(description = "Learner current Java source code")
        String sourceCode,

        @NotNull(message = "Hint level must not be null")
        @Schema(description = "Requested Socratic hint tier", example = "LEVEL_1_NUDGE")
        HintLevel hintLevel,

        @Schema(description = "Optional question or area of confusion from the learner", example = "Why does my loop fail on negative numbers?")
        String userQuestion,

        @Schema(description = "Optional compiler or runtime error message encountered")
        String lastErrorMessage
) {
}
