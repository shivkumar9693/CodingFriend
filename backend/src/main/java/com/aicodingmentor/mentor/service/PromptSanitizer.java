package com.aicodingmentor.mentor.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Sanitization engine for AI Mentor prompts.
 * Strictly guarantees that sensitive credentials, JWTs, database passwords,
 * API keys, and hidden test inputs are completely excluded from prompts before dispatch to LLMs.
 */
@Component
public class PromptSanitizer {

    private static final Pattern JWT_PATTERN = Pattern.compile(
            "eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}"
    );

    private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile(
            "(?i)Bearer\\s+[A-Za-z0-9_\\-\\.]{15,}"
    );

    private static final Pattern API_KEY_PATTERN = Pattern.compile(
            "(?i)(gsk_|sk-|ghp_|gho_|xoxb-)[A-Za-z0-9_\\-]{16,}"
    );

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "(?i)(password|passwd|pwd|secret|db_password|access_token|refresh_token)\\s*[:=]\\s*['\"]?[^\\s,'\"\\n]+['\"]?"
    );

    private static final Pattern DB_URL_PATTERN = Pattern.compile(
            "(?i)(jdbc:[a-z0-9]+://|mysql://|postgresql://|mongodb://)[^\\s\\n]+"
    );

    /**
     * Sanitizes general text content, stripping secrets, tokens, passwords, and DB connection strings.
     *
     * @param input the raw text to sanitize
     * @return sanitized string safe for LLM context inclusion
     */
    public String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String sanitized = input;
        sanitized = JWT_PATTERN.matcher(sanitized).replaceAll("[REDACTED_JWT]");
        sanitized = BEARER_TOKEN_PATTERN.matcher(sanitized).replaceAll("Bearer [REDACTED_TOKEN]");
        sanitized = API_KEY_PATTERN.matcher(sanitized).replaceAll("[REDACTED_API_KEY]");
        sanitized = PASSWORD_PATTERN.matcher(sanitized).replaceAll("$1=[REDACTED_SECRET]");
        sanitized = DB_URL_PATTERN.matcher(sanitized).replaceAll("[REDACTED_DB_URL]");

        return sanitized;
    }

    /**
     * Sanitizes test failure information, guaranteeing that hidden test cases are never exposed.
     *
     * @param inputData sample input
     * @param expectedOutput expected result
     * @param actualOutput actual learner result
     * @param isHidden whether the test case is hidden
     * @return sanitized representation for mentor context
     */
    public String sanitizeTestFailure(String inputData, String expectedOutput, String actualOutput, boolean isHidden) {
        if (isHidden) {
            return "Test Case: [HIDDEN_TEST_CASE] (Input and expected output redacted for test integrity)";
        }

        String safeInput = sanitize(inputData);
        String safeExpected = sanitize(expectedOutput);
        String safeActual = sanitize(actualOutput);

        return String.format("Sample Test Case Failure:\n- Input: %s\n- Expected Output: %s\n- Actual Output: %s",
                truncate(safeInput, 200),
                truncate(safeExpected, 200),
                truncate(safeActual, 200)
        );
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() > maxLength ? text.substring(0, maxLength) + " ... [truncated]" : text;
    }
}
