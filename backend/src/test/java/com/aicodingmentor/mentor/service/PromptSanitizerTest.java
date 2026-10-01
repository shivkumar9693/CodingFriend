package com.aicodingmentor.mentor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PromptSanitizer Unit Tests")
class PromptSanitizerTest {

    private PromptSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new PromptSanitizer();
    }

    @Test
    @DisplayName("Should redact JWT access and refresh tokens")
    void testRedactJwt() {
        String input = "User token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c in header";
        String result = sanitizer.sanitize(input);

        assertThat(result).doesNotContain("eyJhbGciOiJIUzI1NiIs");
        assertThat(result).contains("[REDACTED_JWT]");
    }

    @Test
    @DisplayName("Should redact Bearer authorization tokens")
    void testRedactBearerToken() {
        String input = "Authorization: Bearer mySecretAccessTokenValue123456789";
        String result = sanitizer.sanitize(input);

        assertThat(result).doesNotContain("mySecretAccessTokenValue123456789");
        assertThat(result).contains("Bearer [REDACTED_TOKEN]");
    }

    @Test
    @DisplayName("Should redact API keys (Groq, OpenAI, GitHub)")
    void testRedactApiKeys() {
        String groqKey = "GROQ_API_KEY = gsk_mockSecretKeyForSanitizerTesting1234567890";
        String openAiKey = "OPENAI_KEY = sk-abcdef1234567890abcdef123456";

        assertThat(sanitizer.sanitize(groqKey)).contains("[REDACTED_API_KEY]");
        assertThat(sanitizer.sanitize(openAiKey)).contains("[REDACTED_API_KEY]");
    }

    @Test
    @DisplayName("Should redact database passwords and secret variables")
    void testRedactPasswords() {
        String input = "String password = 'superSecretDbPassword123!'; db_password: secret123";
        String result = sanitizer.sanitize(input);

        assertThat(result).doesNotContain("superSecretDbPassword123!");
        assertThat(result).doesNotContain("secret123");
        assertThat(result).contains("[REDACTED_SECRET]");
    }

    @Test
    @DisplayName("Should redact JDBC and database connection URLs")
    void testRedactDbUrls() {
        String input = "Error connecting to jdbc:mysql://root:pass123@localhost:3306/prod_db";
        String result = sanitizer.sanitize(input);

        assertThat(result).doesNotContain("jdbc:mysql://root:pass123@localhost:3306/prod_db");
        assertThat(result).contains("[REDACTED_DB_URL]");
    }

    @Test
    @DisplayName("Should redact hidden test case data completely")
    void testSanitizeHiddenTestCase() {
        String input = "[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]";
        String expected = "55";
        String actual = "0";

        String result = sanitizer.sanitizeTestFailure(input, expected, actual, true);

        assertThat(result).contains("[HIDDEN_TEST_CASE]");
        assertThat(result).doesNotContain("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]");
        assertThat(result).doesNotContain("55");
    }

    @Test
    @DisplayName("Should format public sample test failure safely")
    void testSanitizePublicTestCase() {
        String input = "[2, 7, 11, 15], target = 9";
        String expected = "[0, 1]";
        String actual = "[]";

        String result = sanitizer.sanitizeTestFailure(input, expected, actual, false);

        assertThat(result).contains("Sample Test Case Failure");
        assertThat(result).contains("[2, 7, 11, 15], target = 9");
        assertThat(result).contains("[0, 1]");
    }
}
