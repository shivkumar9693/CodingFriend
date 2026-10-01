package com.aicodingmentor.auth.service;

import com.aicodingmentor.auth.domain.RefreshToken;
import com.aicodingmentor.auth.exception.InvalidTokenException;
import com.aicodingmentor.auth.repository.RefreshTokenRepository;
import com.aicodingmentor.user.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpirationMs;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.security.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken(
                user,
                UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString(),
                Instant.now().plusMillis(refreshTokenExpirationMs)
        );
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired() || token.isRevoked()) {
            refreshTokenRepository.delete(token);
            throw new InvalidTokenException("Refresh token is expired or revoked. Please sign in again.");
        }
        return token;
    }

    @Transactional
    public void revokeToken(String token) {
        if (token != null && !token.isBlank()) {
            refreshTokenRepository.findByToken(token).ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            });
        }
    }

    @Transactional
    public void deleteByToken(String token) {
        if (token != null && !token.isBlank()) {
            refreshTokenRepository.deleteByToken(token);
        }
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }
}
