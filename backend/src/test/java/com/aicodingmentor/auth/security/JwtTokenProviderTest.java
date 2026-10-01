package com.aicodingmentor.auth.security;

import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private static final String SECRET_KEY = "testSecretKeyForUnitAndIntegrationTestingAtLeast256BitsLong12345";
    private static final long EXPIRATION_MS = 60000; // 1 minute

    private JwtTokenProvider jwtTokenProvider;
    private UserPrincipal userPrincipal;
    private UUID userId;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET_KEY, EXPIRATION_MS);
        userId = UUID.randomUUID();

        User user = new User("developer@aicodingmentor.com", "encodedHash", "Dev User", UserRole.USER);
        user.setId(userId);
        userPrincipal = UserPrincipal.create(user);
    }

    @Test
    @DisplayName("generateAccessToken creates a valid signed JWT containing user claims")
    void testGenerateAccessToken() {
        String token = jwtTokenProvider.generateAccessToken(userPrincipal);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("developer@aicodingmentor.com", jwtTokenProvider.getUsernameFromToken(token));
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
        assertEquals("USER", jwtTokenProvider.getRoleFromToken(token));
    }

    @Test
    @DisplayName("validateToken returns false for malformed or corrupted token")
    void testValidateTokenInvalid() {
        assertFalse(jwtTokenProvider.validateToken("invalid.jwt.token"));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    @DisplayName("validateToken returns false for token signed with a different key")
    void testValidateTokenTamperedSignature() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "differentSecretKeyForUnitTestingWithAtLeast256BitsLength54321",
                EXPIRATION_MS
        );
        String token = otherProvider.generateAccessToken(userPrincipal);

        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("validateToken returns false for expired token")
    void testExpiredToken() {
        // Create a provider with negative expiration to simulate an expired token
        JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET_KEY, -5000);
        String expiredToken = expiredProvider.generateAccessToken(userPrincipal);

        assertFalse(jwtTokenProvider.validateToken(expiredToken));
    }
}
