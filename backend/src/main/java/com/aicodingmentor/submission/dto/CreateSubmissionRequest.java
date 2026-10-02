package com.aicodingmentor.submission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request to submit source code for execution and evaluation")
public record CreateSubmissionRequest(
        @NotNull(message = "problemId is required")
        @Schema(description = "Unique identifier of the problem", example = "8e177368-aba4-43ce-bb1b-32ee2ffa1f80")
        UUID problemId,

        @NotBlank(message = "sourceCode must not be blank")
        @Size(max = 65535, message = "sourceCode length must not exceed 65,535 characters")
        @Schema(description = "Learner source code to evaluate", example = "class Solution { public int[] twoSum(int[] nums, int target) { return new int[]{0, 1}; } }")
        String sourceCode,

        @Pattern(regexp = "^(?i)(JAVA|PYTHON|CPP|JAVASCRIPT)$", message = "Supported languages: JAVA, PYTHON, CPP, JAVASCRIPT")
        @Schema(description = "Programming language (JAVA, PYTHON, CPP, JAVASCRIPT)", example = "JAVA", defaultValue = "JAVA")
        String language
) {
    public CreateSubmissionRequest {
        if (language == null || language.isBlank()) {
            language = "JAVA";
        } else {
            language = language.toUpperCase();
        }
    }
}
