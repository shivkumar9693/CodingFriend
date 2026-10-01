package com.aicodingmentor.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for updating user profile")
public record UpdateProfileRequest(
        @Schema(description = "User's full name or display handle", example = "Alex Mercer")
        @NotBlank(message = "Full name cannot be blank")
        @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
        String fullName,

        @Schema(description = "Short biography or coding interests", example = "Java backend enthusiast & problem solver")
        @Size(max = 500, message = "Bio cannot exceed 500 characters")
        String bio,

        @Schema(description = "GitHub handle without URL", example = "alexmercer-dev")
        @Size(max = 100, message = "GitHub username cannot exceed 100 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "GitHub username can only contain alphanumeric characters, hyphens, and underscores")
        String githubUsername
) {
}
