package com.aicodingmentor.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload for Google OAuth2 / ID token authentication")
public record GoogleLoginRequest(
        @Schema(description = "Google ID Token (JWT) issued by Google Identity Services", example = "eyJhbGciOiJSUzI1NiIs...")
        @NotBlank(message = "Google ID token is required")
        String idToken
) {
}
