package com.aicodingmentor.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload after initiating a password reset request")
public record ForgotPasswordResponse(
        @Schema(description = "User-facing message", example = "If an account exists with this email, password reset instructions have been generated.")
        String message,

        @Schema(description = "Reset token (provided for testing/direct link resolution)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String resetToken
) {
    public static ForgotPasswordResponse of(String message, String resetToken) {
        return new ForgotPasswordResponse(message, resetToken);
    }
}
