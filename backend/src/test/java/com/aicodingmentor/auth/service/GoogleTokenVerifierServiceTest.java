package com.aicodingmentor.auth.service;

import com.aicodingmentor.auth.dto.GoogleUserInfo;
import com.aicodingmentor.auth.exception.InvalidTokenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleTokenVerifierServiceTest {

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    private GoogleTokenVerifierService googleTokenVerifierService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        when(restTemplateBuilder.setConnectTimeout(any(Duration.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.setReadTimeout(any(Duration.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        objectMapper = new ObjectMapper();
        googleTokenVerifierService = new GoogleTokenVerifierService(restTemplateBuilder, objectMapper);
    }

    @Test
    @DisplayName("verifyToken successfully parses valid Google tokeninfo response")
    void testVerifyTokenSuccess() {
        String json = """
                {
                  "iss": "https://accounts.google.com",
                  "sub": "123456789",
                  "email": "coder@example.com",
                  "email_verified": "true",
                  "name": "Alex Coder",
                  "picture": "https://example.com/pic.jpg",
                  "given_name": "Alex",
                  "family_name": "Coder"
                }
                """;

        when(restTemplate.getForEntity(eq("https://oauth2.googleapis.com/tokeninfo?id_token=valid_token"), eq(String.class)))
                .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        GoogleUserInfo info = googleTokenVerifierService.verifyToken("valid_token");

        assertNotNull(info);
        assertEquals("coder@example.com", info.email());
        assertEquals("123456789", info.sub());
        assertEquals("Alex Coder", info.name());
        assertTrue(info.emailVerified());
        assertEquals("https://example.com/pic.jpg", info.picture());
    }

    @Test
    @DisplayName("verifyToken throws InvalidTokenException on unverified email")
    void testVerifyTokenUnverifiedEmail() {
        String json = """
                {
                  "iss": "https://accounts.google.com",
                  "sub": "123456789",
                  "email": "unverified@example.com",
                  "email_verified": "false",
                  "name": "Unverified User"
                }
                """;

        when(restTemplate.getForEntity(eq("https://oauth2.googleapis.com/tokeninfo?id_token=bad_token"), eq(String.class)))
                .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        assertThrows(InvalidTokenException.class, () -> googleTokenVerifierService.verifyToken("bad_token"));
    }

    @Test
    @DisplayName("verifyToken throws InvalidTokenException on invalid issuer")
    void testVerifyTokenInvalidIssuer() {
        String json = """
                {
                  "iss": "https://attacker.com",
                  "sub": "123456789",
                  "email": "attacker@example.com",
                  "email_verified": "true"
                }
                """;

        when(restTemplate.getForEntity(eq("https://oauth2.googleapis.com/tokeninfo?id_token=fake_token"), eq(String.class)))
                .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        assertThrows(InvalidTokenException.class, () -> googleTokenVerifierService.verifyToken("fake_token"));
    }

    @Test
    @DisplayName("verifyToken throws InvalidTokenException for blank token")
    void testVerifyTokenBlank() {
        assertThrows(InvalidTokenException.class, () -> googleTokenVerifierService.verifyToken("   "));
    }
}
