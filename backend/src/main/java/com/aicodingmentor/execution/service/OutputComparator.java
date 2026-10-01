package com.aicodingmentor.execution.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class OutputComparator {

    /**
     * Compare expected and actual outputs with whitespace and format normalization.
     * Normalizes line breaks, trims trailing whitespace on each line,
     * trims trailing blank lines, and normalizes array comma spacing (e.g. [0, 1] vs [0,1]).
     *
     * @param expected the expected output string
     * @param actual the actual output string produced by the program
     * @return true if normalized outputs are identical, false otherwise
     */
    public boolean matches(String expected, String actual) {
        if (expected == null && actual == null) return true;
        if ((expected == null && actual != null && !actual.trim().isEmpty()) ||
            (actual == null && expected != null && !expected.trim().isEmpty())) {
            return false;
        }

        String normalizedExpected = normalize(expected);
        String normalizedActual = normalize(actual);

        if (Objects.equals(normalizedExpected, normalizedActual)) {
            return true;
        }

        // Canonical comparison (stripping spaces around commas inside brackets/lists)
        String canonicalExpected = canonicalize(normalizedExpected);
        String canonicalActual = canonicalize(normalizedActual);

        return Objects.equals(canonicalExpected, canonicalActual);
    }

    /**
     * Strict equality comparison without whitespace normalization.
     */
    public boolean strictMatches(String expected, String actual) {
        if (expected == null && actual == null) return true;
        if (expected == null || actual == null) return false;
        return expected.equals(actual);
    }

    /**
     * Normalizes an output string:
     * 1. Replaces Windows (\r\n) and classic Mac (\r) line breaks with Unix (\n).
     * 2. Trims trailing whitespace from each line.
     * 3. Trims trailing empty lines at the end.
     */
    public String normalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String unifiedNewlines = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] rawLines = unifiedNewlines.split("\n", -1);

        List<String> trimmedLines = new ArrayList<>(rawLines.length);
        for (String line : rawLines) {
            trimmedLines.add(trimTrailingWhitespace(line));
        }

        // Remove trailing empty lines from the end
        int lastIndex = trimmedLines.size() - 1;
        while (lastIndex >= 0 && trimmedLines.get(lastIndex).isEmpty()) {
            lastIndex--;
        }

        if (lastIndex < 0) {
            return "";
        }

        return String.join("\n", trimmedLines.subList(0, lastIndex + 1));
    }

    private String canonicalize(String text) {
        if (text == null) return "";
        // Normalize array spacing: "[0, 1]" -> "[0,1]"
        return text.replaceAll("\\s*,\\s*", ",")
                .replaceAll("\\[\\s+", "[")
                .replaceAll("\\s+\\]", "]")
                .trim();
    }

    private String trimTrailingWhitespace(String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }
        int len = line.length();
        int st = 0;
        while ((st < len) && (Character.isWhitespace(line.charAt(len - 1)))) {
            len--;
        }
        return (len < line.length()) ? line.substring(0, len) : line;
    }
}
