package com.aicodingmentor.learning.domain;

/**
 * Diagnostic categorization for counter-examples and problem post-mortems.
 */
public enum MistakeCategory {
    LOGIC_ERROR,
    OFF_BY_ONE,
    CORNER_CASE,
    TIME_LIMIT_EXCEEDED,
    RUNTIME_EXCEPTION,
    SYNTAX_ERROR,
    INFINITE_LOOP,
    MEMORY_LIMIT,
    TYPE_MISMATCH,
    RECURSION_BASE_CASE,
    TWO_POINTER_INVARIANT,
    INTEGER_OVERFLOW;

    public static MistakeCategory fromString(String val) {
        if (val == null || val.isBlank()) {
            return LOGIC_ERROR;
        }
        try {
            return MistakeCategory.valueOf(val.trim().toUpperCase().replace(" ", "_").replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return LOGIC_ERROR;
        }
    }
}
