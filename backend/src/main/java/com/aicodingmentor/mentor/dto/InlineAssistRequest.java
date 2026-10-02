package com.aicodingmentor.mentor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request for real-time Socratic inline coding assistance inside the editor")
public record InlineAssistRequest(
        @NotNull(message = "problemId is required")
        @Schema(description = "Unique identifier of the problem", example = "8e177368-aba4-43ce-bb1b-32ee2ffa1f80")
        UUID problemId,

        @NotBlank(message = "sourceCode must not be blank")
        @Size(max = 65535, message = "sourceCode length must not exceed 65,535 characters")
        @Schema(description = "Current source code in the editor")
        String sourceCode,

        @Min(value = 1, message = "cursorLine must be at least 1")
        @Schema(description = "1-based line number where the learner cursor is located", example = "5")
        int cursorLine,

        @Pattern(regexp = "^(?i)(JAVA|PYTHON|CPP|JAVASCRIPT)$", message = "Supported languages: JAVA, PYTHON, CPP, JAVASCRIPT")
        @Schema(description = "Programming language (JAVA, PYTHON, CPP, JAVASCRIPT)", example = "JAVA", defaultValue = "JAVA")
        String language
) {
    public InlineAssistRequest {
        if (language == null || language.isBlank()) {
            language = "JAVA";
        } else {
            language = language.toUpperCase();
        }
    }
}
