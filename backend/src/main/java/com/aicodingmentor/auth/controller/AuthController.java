package com.aicodingmentor.auth.controller;

import com.aicodingmentor.auth.dto.AuthResponse;
import com.aicodingmentor.auth.dto.GoogleLoginRequest;
import com.aicodingmentor.auth.dto.LoginRequest;
import com.aicodingmentor.auth.dto.LogoutRequest;
import com.aicodingmentor.auth.dto.RefreshTokenRequest;
import com.aicodingmentor.auth.dto.RegisterRequest;
import com.aicodingmentor.auth.dto.TokenRefreshResponse;
import com.aicodingmentor.auth.service.AuthService;
import com.aicodingmentor.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/auth", "/api/v1/auth"})
@Tag(name = "Authentication", description = "Endpoints for user registration, authentication, token refresh, and logout")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account, hashes password, and issues access & refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Verifies user credentials and returns access & refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("User authenticated successfully", response));
    }

    @PostMapping("/google")
    @Operation(summary = "Authenticate with Google", description = "Verifies Google ID token and returns application access & refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.loginWithGoogle(request);
        return ResponseEntity.ok(ApiResponse.success("Authenticated with Google successfully", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Generates a new access token using a valid refresh token")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenRefreshResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes refresh token and clears session authentication")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) LogoutRequest request) {
        authService.logout(request != null ? request : new LogoutRequest(null));
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
}
