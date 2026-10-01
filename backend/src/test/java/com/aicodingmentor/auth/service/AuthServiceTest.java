package com.aicodingmentor.auth.service;

import com.aicodingmentor.auth.domain.RefreshToken;
import com.aicodingmentor.auth.dto.AuthResponse;
import com.aicodingmentor.auth.dto.LoginRequest;
import com.aicodingmentor.auth.dto.LogoutRequest;
import com.aicodingmentor.auth.dto.RefreshTokenRequest;
import com.aicodingmentor.auth.dto.RegisterRequest;
import com.aicodingmentor.auth.dto.TokenRefreshResponse;
import com.aicodingmentor.auth.exception.DuplicateEmailException;
import com.aicodingmentor.auth.exception.InvalidTokenException;
import com.aicodingmentor.auth.security.JwtTokenProvider;
import com.aicodingmentor.auth.security.UserPrincipal;
import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.domain.UserRole;
import com.aicodingmentor.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtTokenProvider,
                refreshTokenService
        );
    }

    @Test
    @DisplayName("register successfully encodes password, persists user, and returns tokens without exposing password hash")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest("newlearner@example.com", "Password123!", "Learner One", UserRole.USER);

        when(userRepository.existsByEmail("newlearner@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("bcrypt_hashed_password");

        User savedUser = new User("newlearner@example.com", "bcrypt_hashed_password", "Learner One", UserRole.USER);
        UUID generatedId = UUID.randomUUID();
        savedUser.setId(generatedId);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("sample.access.token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);

        RefreshToken refreshToken = new RefreshToken(savedUser, "sample-refresh-token", Instant.now().plusSeconds(3600));
        when(refreshTokenService.createRefreshToken(savedUser)).thenReturn(refreshToken);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("sample.access.token", response.accessToken());
        assertEquals("sample-refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900000L, response.expiresIn());
        assertNotNull(response.user());
        assertEquals("newlearner@example.com", response.user().email());
        assertEquals("Learner One", response.user().fullName());
        assertEquals(UserRole.USER, response.user().role());

        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register throws DuplicateEmailException when email is already registered")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("existing@example.com", "Password123!", "Existing User", UserRole.USER);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("login successfully authenticates user and returns access and refresh tokens")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("user@example.com", "Password123!");

        UUID userId = UUID.randomUUID();
        User user = new User("user@example.com", "hashed_pass", "John Doe", UserRole.USER);
        user.setId(userId);
        UserPrincipal userPrincipal = UserPrincipal.create(user);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(userPrincipal)).thenReturn("jwt.token.here");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);

        RefreshToken refreshToken = new RefreshToken(user, "refresh-token-val", Instant.now().plusSeconds(3600));
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt.token.here", response.accessToken());
        assertEquals("refresh-token-val", response.refreshToken());
        assertEquals("user@example.com", response.user().email());
    }

    @Test
    @DisplayName("login propagates BadCredentialsException on authentication failure")
    void testLoginBadCredentials() {
        LoginRequest request = new LoginRequest("wrong@example.com", "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("refreshToken generates a new access token for a valid refresh token")
    void testRefreshTokenSuccess() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        User user = new User("user@example.com", "hash", "John", UserRole.USER);
        user.setId(UUID.randomUUID());
        RefreshToken refreshToken = new RefreshToken(user, "valid-refresh-token", Instant.now().plusSeconds(3600));

        when(refreshTokenService.findByToken("valid-refresh-token")).thenReturn(Optional.of(refreshToken));
        when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(refreshToken);
        when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("new.access.token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);

        TokenRefreshResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new.access.token", response.accessToken());
        assertEquals("valid-refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
    }

    @Test
    @DisplayName("refreshToken throws InvalidTokenException when refresh token is unknown")
    void testRefreshTokenNotFound() {
        RefreshTokenRequest request = new RefreshTokenRequest("unknown-token");
        when(refreshTokenService.findByToken("unknown-token")).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> authService.refreshToken(request));
    }

    @Test
    @DisplayName("logout revokes refresh token and clears context")
    void testLogout() {
        LogoutRequest request = new LogoutRequest("token-to-invalidate");

        authService.logout(request);

        verify(refreshTokenService).deleteByToken("token-to-invalidate");
    }
}
