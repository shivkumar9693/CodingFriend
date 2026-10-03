package com.aicodingmentor.auth.service;

import com.aicodingmentor.auth.domain.PasswordResetToken;
import com.aicodingmentor.auth.dto.ChangePasswordRequest;
import com.aicodingmentor.auth.dto.ForgotPasswordRequest;
import com.aicodingmentor.auth.dto.ForgotPasswordResponse;
import com.aicodingmentor.auth.dto.ResetPasswordRequest;
import com.aicodingmentor.auth.exception.InvalidTokenException;
import com.aicodingmentor.auth.repository.PasswordResetTokenRepository;
import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.domain.UserRole;
import com.aicodingmentor.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetServiceImpl(
                userRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                refreshTokenService,
                eventPublisher
        );
    }

    @Test
    @DisplayName("requestPasswordReset creates token when email exists")
    void testRequestPasswordResetSuccess() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("learner@example.com");
        User user = new User("learner@example.com", "hash", "Learner", UserRole.USER);
        user.setId(UUID.randomUUID());

        when(userRepository.findByEmail("learner@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ForgotPasswordResponse response = passwordResetService.requestPasswordReset(request);

        assertNotNull(response);
        assertNotNull(response.resetToken());
        verify(passwordResetTokenRepository).deleteByUser(user);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("requestPasswordReset returns generic response when email does not exist without error")
    void testRequestPasswordResetUnknownEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("unknown@example.com");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        ForgotPasswordResponse response = passwordResetService.requestPasswordReset(request);

        assertNotNull(response);
        assertNull(response.resetToken());
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("resetPassword successfully updates password and marks token used")
    void testResetPasswordSuccess() {
        ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "NewPassword123!");
        User user = new User("learner@example.com", "old-hash", "Learner", UserRole.USER);
        user.setId(UUID.randomUUID());

        PasswordResetToken token = new PasswordResetToken(user, "valid-token", Instant.now().plusSeconds(1800));

        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("new-bcrypt-hash");

        passwordResetService.resetPassword(request);

        assertEquals("new-bcrypt-hash", user.getPasswordHash());
        assertTrue(token.isUsed());
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(token);
        verify(refreshTokenService).revokeAllUserTokens(user);
    }

    @Test
    @DisplayName("resetPassword throws InvalidTokenException when token is expired")
    void testResetPasswordExpiredToken() {
        ResetPasswordRequest request = new ResetPasswordRequest("expired-token", "NewPassword123!");
        User user = new User("learner@example.com", "old-hash", "Learner", UserRole.USER);

        PasswordResetToken token = new PasswordResetToken(user, "expired-token", Instant.now().minusSeconds(60));

        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThrows(InvalidTokenException.class, () -> passwordResetService.resetPassword(request));
        verify(passwordResetTokenRepository).delete(token);
    }

    @Test
    @DisplayName("changePassword updates password when old password matches")
    void testChangePasswordSuccess() {
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123!", "NewPassword123!");
        User user = new User("learner@example.com", "current-hash", "Learner", UserRole.USER);
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword123!", "current-hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("new-hash");

        passwordResetService.changePassword(userId, request);

        assertEquals("new-hash", user.getPasswordHash());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("changePassword throws BadCredentialsException when old password is wrong")
    void testChangePasswordWrongOldPassword() {
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest("WrongPassword!", "NewPassword123!");
        User user = new User("learner@example.com", "current-hash", "Learner", UserRole.USER);
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword!", "current-hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> passwordResetService.changePassword(userId, request));
        verify(userRepository, never()).save(any());
    }
}
