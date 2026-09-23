package com.aicodingmentor.common.error;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        boolean success,
        int status,
        String error,
        String message,
        Instant timestamp,
        List<ValidationError> validationErrors
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(false, status, error, message, Instant.now(), List.of());
    }

    public static ErrorResponse of(int status, String error, String message, List<ValidationError> errors) {
        return new ErrorResponse(false, status, error, message, Instant.now(), errors);
    }

    public record ValidationError(String field, String message) {}
}
