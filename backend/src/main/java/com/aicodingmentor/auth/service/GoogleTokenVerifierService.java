package com.aicodingmentor.auth.service;

import com.aicodingmentor.auth.dto.GoogleUserInfo;
import com.aicodingmentor.auth.exception.InvalidTokenException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Service
public class GoogleTokenVerifierService {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifierService.class);
    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.security.oauth2.google.client-id:}")
    private String googleClientId;

    public GoogleTokenVerifierService(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Verifies a Google ID Token against Google's public OAuth2 tokeninfo service.
     *
     * @param idToken The raw ID token from Google Identity Services
     * @return Validated GoogleUserInfo containing user claims
     * @throws InvalidTokenException if token is invalid, expired, or failed verification
     */
    public GoogleUserInfo verifyToken(String idToken) {
        if (!StringUtils.hasText(idToken)) {
            throw new InvalidTokenException("Google ID token cannot be empty");
        }

        try {
            String url = GOOGLE_TOKENINFO_URL + idToken.trim();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new InvalidTokenException("Failed to verify Google ID token with provider");
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            // Validate error fields if present
            if (root.has("error") || root.has("error_description")) {
                String error = root.path("error_description").asText(root.path("error").asText("Invalid token"));
                log.warn("Google token verification rejected by provider: {}", error);
                throw new InvalidTokenException("Google authentication failed: " + error);
            }

            String iss = root.path("iss").asText("");
            if (!"https://accounts.google.com".equals(iss) && !"accounts.google.com".equals(iss)) {
                log.warn("Invalid Google token issuer: {}", iss);
                throw new InvalidTokenException("Invalid token issuer: not issued by Google");
            }

            // If googleClientId is configured, verify the audience
            if (StringUtils.hasText(googleClientId)) {
                String aud = root.path("aud").asText("");
                if (!googleClientId.trim().equals(aud)) {
                    log.warn("Google token audience mismatch. Expected: {}, Received: {}", googleClientId, aud);
                    throw new InvalidTokenException("Google token audience mismatch");
                }
            }

            String email = root.path("email").asText(null);
            if (!StringUtils.hasText(email)) {
                throw new InvalidTokenException("Google account does not have a verified email address");
            }

            boolean emailVerified = root.path("email_verified").asBoolean(
                    "true".equalsIgnoreCase(root.path("email_verified").asText("false"))
            );

            if (!emailVerified) {
                throw new InvalidTokenException("Google email address is not verified");
            }

            String sub = root.path("sub").asText("");
            String name = root.path("name").asText(email.split("@")[0]);
            String picture = root.path("picture").asText(null);
            String givenName = root.path("given_name").asText(null);
            String familyName = root.path("family_name").asText(null);

            return new GoogleUserInfo(sub, email.toLowerCase().trim(), emailVerified, name, picture, givenName, familyName);

        } catch (HttpStatusCodeException ex) {
            log.warn("Google OAuth tokeninfo endpoint returned error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new InvalidTokenException("Google token is invalid, expired, or revoked");
        } catch (InvalidTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error verifying Google ID token", ex);
            throw new InvalidTokenException("Unable to complete Google authentication at this time");
        }
    }
}
