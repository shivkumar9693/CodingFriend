package com.aicodingmentor.auth.service;

import com.aicodingmentor.auth.domain.RefreshToken;
import com.aicodingmentor.auth.exception.InvalidTokenException;
import com.aicodingmentor.auth.repository.RefreshTokenRepository;
import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;
    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, 604800000L);
        user = new User("user@aicodingmentor.com", "hash", "Learner", UserRole.USER);
        user.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("createRefreshToken saves and returns a new active refresh token")
    void testCreateRefreshToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        assertNotNull(refreshToken);
        assertNotNull(refreshToken.getToken());
        assertEquals(user, refreshToken.getUser());
        assertFalse(refreshToken.isRevoked());
        assertTrue(refreshToken.getExpiresAt().isAfter(Instant.now()));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("verifyExpiration succeeds for valid non-expired token")
    void testVerifyExpirationValid() {
        RefreshToken token = new RefreshToken(user, "valid-token", Instant.now().plusSeconds(3600));

        RefreshToken verified = refreshTokenService.verifyExpiration(token);

        assertEquals(token, verified);
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    @DisplayName("verifyExpiration throws InvalidTokenException and deletes expired token")
    void testVerifyExpirationExpired() {
        RefreshToken token = new RefreshToken(user, "expired-token", Instant.now().minusSeconds(10));

        assertThrows(InvalidTokenException.class, () -> refreshTokenService.verifyExpiration(token));
        verify(refreshTokenRepository).delete(token);
    }

    @Test
    @DisplayName("verifyExpiration throws InvalidTokenException and deletes revoked token")
    void testVerifyExpirationRevoked() {
        RefreshToken token = new RefreshToken(user, "revoked-token", Instant.now().plusSeconds(3600));
        token.setRevoked(true);

        assertThrows(InvalidTokenException.class, () -> refreshTokenService.verifyExpiration(token));
        verify(refreshTokenRepository).delete(token);
    }

    @Test
    @DisplayName("revokeToken marks token as revoked and persists update")
    void testRevokeToken() {
        RefreshToken token = new RefreshToken(user, "token-to-revoke", Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByToken("token-to-revoke")).thenReturn(Optional.of(token));

        refreshTokenService.revokeToken("token-to-revoke");

        assertTrue(token.isRevoked());
        verify(refreshTokenRepository).save(token);
    }

    @Test
    @DisplayName("deleteByToken deletes token from database")
    void testDeleteByToken() {
        refreshTokenService.deleteByToken("token-to-delete");

        verify(refreshTokenRepository).deleteByToken("token-to-delete");
    }
}
