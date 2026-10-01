package com.aicodingmentor.mentor.dto;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request body for diagnosing a failed submission attempt with SocraBuddy")
public record SocraBuddyDiagnosisRequest(
        @NotNull(message = "Problem ID must not be null")
        @Schema(description = "Target problem ID")
        UUID problemId,

        @Schema(description = "Optional submission ID")
        UUID submissionId,

        @NotBlank(message = "Source code must not be blank")
        @Schema(description = "Submitted source code")
        String sourceCode,

        @Schema(description = "Evaluation verdict received", example = "WRONG_ANSWER")
        EvaluationVerdict verdict,

        @Schema(description = "Error output or compiler trace")
        String errorMessage,

        @Schema(description = "Test case number that failed", example = "3")
        Integer failedTestNumber
) {
}
