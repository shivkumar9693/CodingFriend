package com.aicodingmentor.learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

@Schema(description = "Request to analyze a failed submission and generate an AI counter-example discrepancy post-mortem")
public record GeneratePostMortemRequest(
        @Schema(description = "Problem ID", example = "8e177368-aba4-43ce-bb1b-32ee2ffa1f80")
        UUID problemId,

        @Schema(description = "Problem Title", example = "Two Sum")
        String problemTitle,

        @Schema(description = "Programming Language", example = "JAVA")
        String language,

        @NotBlank(message = "Submitted code is required")
        @Schema(description = "Learner source code")
        String submittedCode,

        @Schema(description = "Execution verdict", example = "WRONG_ANSWER")
        String verdict,

        @Schema(description = "Failed test case input", example = "nums = [3, 2, 4], target = 6")
        String failedInput,

        @Schema(description = "Actual output produced by learner's code", example = "[0, 0]")
        String actualOutput,

        @Schema(description = "Expected output required by problem spec", example = "[1, 2]")
        String expectedOutput,

        @Schema(description = "Runtime or compiler error message if any")
        String errorMessage
) {
}
