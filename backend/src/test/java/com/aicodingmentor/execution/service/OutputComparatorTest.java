package com.aicodingmentor.execution.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OutputComparator Unit Tests")
class OutputComparatorTest {

    private OutputComparator comparator;

    @BeforeEach
    void setUp() {
        comparator = new OutputComparator();
    }

    @Test
    @DisplayName("Should match exact identical strings")
    void testExactMatch() {
        assertThat(comparator.matches("42", "42")).isTrue();
        assertThat(comparator.matches("Hello World", "Hello World")).isTrue();
        assertThat(comparator.matches("1 2 3\n4 5 6", "1 2 3\n4 5 6")).isTrue();
    }

    @Test
    @DisplayName("Should normalize trailing whitespace on lines")
    void testTrailingWhitespaceHandling() {
        assertThat(comparator.matches("42", "42   ")).isTrue();
        assertThat(comparator.matches("Line 1\nLine 2", "Line 1  \nLine 2    ")).isTrue();
    }

    @Test
    @DisplayName("Should normalize Windows CRLF, Mac CR, and Unix LF line endings")
    void testLineEndingNormalization() {
        assertThat(comparator.matches("Line 1\nLine 2\n", "Line 1\r\nLine 2\r\n")).isTrue();
        assertThat(comparator.matches("A\nB\nC", "A\rB\rC")).isTrue();
    }

    @Test
    @DisplayName("Should ignore trailing empty lines / newlines")
    void testTrailingEmptyLinesHandling() {
        assertThat(comparator.matches("Output", "Output\n\n\n")).isTrue();
        assertThat(comparator.matches("Output\n", "Output")).isTrue();
        assertThat(comparator.matches("Line 1\nLine 2\n", "Line 1\nLine 2\n\n\n")).isTrue();
    }

    @Test
    @DisplayName("Should detect actual output mismatches")
    void testMismatchDetection() {
        assertThat(comparator.matches("42", "43")).isFalse();
        assertThat(comparator.matches("true", "false")).isFalse();
        assertThat(comparator.matches("Hello", "hello")).isFalse(); // Case sensitive
        assertThat(comparator.matches("[1, 2]", "[2, 1]")).isFalse();
    }

    @Test
    @DisplayName("Should handle null and empty strings gracefully")
    void testNullAndEmptyHandling() {
        assertThat(comparator.matches("", "")).isTrue();
        assertThat(comparator.matches(null, "")).isTrue();
        assertThat(comparator.matches("", null)).isTrue();
        assertThat(comparator.matches(null, null)).isTrue();
        assertThat(comparator.matches("42", null)).isFalse();
        assertThat(comparator.matches(null, "42")).isFalse();
    }
}
