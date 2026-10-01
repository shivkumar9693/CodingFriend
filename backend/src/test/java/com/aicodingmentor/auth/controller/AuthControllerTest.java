package com.aicodingmentor.auth.controller;

import com.aicodingmentor.auth.dto.AuthResponse;
import com.aicodingmentor.auth.dto.LoginRequest;
import com.aicodingmentor.auth.dto.LogoutRequest;
import com.aicodingmentor.auth.dto.RefreshTokenRequest;
import com.aicodingmentor.auth.dto.RegisterRequest;
import com.aicodingmentor.auth.dto.TokenRefreshResponse;
import com.aicodingmentor.auth.dto.UserDto;
import com.aicodingmentor.auth.exception.DuplicateEmailException;
import com.aicodingmentor.auth.exception.InvalidTokenException;
import com.aicodingmentor.auth.security.CustomUserDetailsService;
import com.aicodingmentor.auth.security.JwtAccessDeniedHandler;
import com.aicodingmentor.auth.security.JwtAuthenticationEntryPoint;
import com.aicodingmentor.auth.security.JwtAuthenticationFilter;
import com.aicodingmentor.auth.security.JwtTokenProvider;
import com.aicodingmentor.auth.service.AuthService;
import com.aicodingmentor.common.error.GlobalExceptionHandler;
import com.aicodingmentor.user.domain.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("POST /api/auth/register returns 201 Created on valid registration")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest("learner@example.com", "Password123!", "Learner Name", UserRole.USER);
        UserDto userDto = new UserDto(UUID.randomUUID(), "learner@example.com", "Learner Name", UserRole.USER);
        AuthResponse authResponse = AuthResponse.of("mock.access.token", "mock.refresh.token", 900000L, userDto);

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("mock.access.token"))
                .andExpect(jsonPath("$.data.refreshToken").value("mock.refresh.token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("learner@example.com"))
                .andExpect(jsonPath("$.data.user.fullName").value("Learner Name"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/register returns 400 Bad Request when validation fails")
    void testRegisterValidationFailure() throws Exception {
        // Invalid email, short password, blank name
        RegisterRequest request = new RegisterRequest("invalid-email", "short", "", null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.validationErrors", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("POST /api/auth/register returns 409 Conflict on duplicate email")
    void testRegisterDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("existing@example.com", "Password123!", "Jane Doe", UserRole.USER);

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(DuplicateEmailException.forEmail("existing@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    @DisplayName("POST /api/auth/login returns 200 OK on valid credentials")
    void testLoginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("learner@example.com", "Password123!");
        UserDto userDto = new UserDto(UUID.randomUUID(), "learner@example.com", "Learner Name", UserRole.USER);
        AuthResponse authResponse = AuthResponse.of("mock.access.token", "mock.refresh.token", 900000L, userDto);

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock.access.token"))
                .andExpect(jsonPath("$.data.refreshToken").value("mock.refresh.token"))
                .andExpect(jsonPath("$.data.user.email").value("learner@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/login returns 401 Unauthorized on invalid credentials")
    void testLoginInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("learner@example.com", "WrongPassword!");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message", containsString("Invalid email or password")));
    }

    @Test
    @DisplayName("POST /api/auth/refresh returns 200 OK on valid refresh token")
    void testRefreshTokenSuccess() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token-value");
        TokenRefreshResponse refreshResponse = TokenRefreshResponse.of("new.access.token", "valid-refresh-token-value", 900000L);

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(refreshResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new.access.token"))
                .andExpect(jsonPath("$.data.refreshToken").value("valid-refresh-token-value"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh returns 401 Unauthorized on invalid/expired refresh token")
    void testRefreshTokenInvalid() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("expired-token");

        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new InvalidTokenException("Refresh token is expired or revoked"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/auth/logout returns 200 OK")
    void testLogoutSuccess() throws Exception {
        LogoutRequest request = new LogoutRequest("valid-token");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(authService).logout(any(LogoutRequest.class));
    }
}
