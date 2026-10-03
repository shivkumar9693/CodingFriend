package com.aicodingmentor.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to complete a password reset with a valid token")
public record ResetPasswordRequest(
        @Schema(description = "Password reset token received via link or email", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        @NotBlank(message = "Reset token is required")
        String token,

        @Schema(description = "New password (minimum 8 characters)", example = "NewSecretPassword123!")
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String newPassword
) {
}
