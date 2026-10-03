package com.aicodingmentor.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload to initiate a password reset workflow")
public record ForgotPasswordRequest(
        @Schema(description = "Registered email address of the account", example = "learner@example.com")
        @NotBlank(message = "Email address is required")
        @Email(message = "Email address must be well-formed")
        String email
) {
}
