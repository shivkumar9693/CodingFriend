package com.aicodingmentor.auth.service;

import com.aicodingmentor.auth.domain.PasswordResetToken;
import com.aicodingmentor.auth.dto.ChangePasswordRequest;
import com.aicodingmentor.auth.dto.ForgotPasswordRequest;
import com.aicodingmentor.auth.dto.ForgotPasswordResponse;
import com.aicodingmentor.auth.dto.ResetPasswordRequest;
import com.aicodingmentor.auth.exception.InvalidTokenException;
import com.aicodingmentor.auth.repository.PasswordResetTokenRepository;
import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetServiceImpl.class);
    private static final Duration TOKEN_EXPIRY_DURATION = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @org.springframework.beans.factory.annotation.Value("${app.frontend-url:https://codingfriend.vercel.app}")
    private String frontendUrl;

    public PasswordResetServiceImpl(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            org.springframework.context.ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ForgotPasswordResponse requestPasswordReset(ForgotPasswordRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);

        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", normalizedEmail);
            // Return generic message to prevent email harvesting/enumeration attacks
            return ForgotPasswordResponse.of(
                    "If an account exists with this email, password reset instructions have been generated.",
                    null
            );
        }

        User user = userOpt.get();

        // Delete any existing reset tokens for this user
        passwordResetTokenRepository.deleteByUser(user);

        // Generate cryptographically secure token and 30-minute expiry
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        Instant expiryDate = Instant.now().plus(TOKEN_EXPIRY_DURATION);

        PasswordResetToken resetToken = new PasswordResetToken(user, token, expiryDate);
        passwordResetTokenRepository.save(resetToken);

        String resetLink = (frontendUrl != null ? frontendUrl : "https://codingfriend.vercel.app") + "/reset-password?token=" + token;
        log.info("Generated password reset token for user {}. Link: {}", user.getEmail(), resetLink);

        // Publish event for asynchronous email notification
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new com.aicodingmentor.notification.event.PasswordResetRequestedEvent(
                    user.getEmail(),
                    user.getFullName(),
                    resetLink
            ));
        }

        return ForgotPasswordResponse.of(
                "If an account exists with this email, password reset instructions have been sent.",
                token
        );
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String rawToken = request.token().trim();

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid, expired, or already used reset token. Please request a new password reset."));

        if (resetToken.isUsed()) {
            throw new InvalidTokenException("This password reset token has already been used.");
        }

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new InvalidTokenException("Password reset link has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        String encodedPassword = passwordEncoder.encode(request.newPassword());
        user.setPasswordHash(encodedPassword);
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Revoke active sessions/refresh tokens for security
        refreshTokenService.revokeAllUserTokens(user);

        log.info("Password successfully reset for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("User account not found"));

        if (user.getPasswordHash() != null && !passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        String encodedPassword = passwordEncoder.encode(request.newPassword());
        user.setPasswordHash(encodedPassword);
        userRepository.save(user);

        log.info("Password successfully changed for user: {}", user.getEmail());
    }
}
