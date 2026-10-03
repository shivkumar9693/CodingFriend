package com.aicodingmentor.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for an authenticated user to change their current password")
public record ChangePasswordRequest(
        @Schema(description = "Current password", example = "OldPassword123!")
        @NotBlank(message = "Current password is required")
        String oldPassword,

        @Schema(description = "New password (minimum 8 characters)", example = "NewSecretPassword123!")
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String newPassword
) {
}
