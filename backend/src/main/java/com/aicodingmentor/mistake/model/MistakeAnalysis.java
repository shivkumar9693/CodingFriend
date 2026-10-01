package com.aicodingmentor.mistake.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Objects;

@Schema(description = "Deterministic mistake analysis outcome")
public record MistakeAnalysis(
        @Schema(description = "Classified mistake type", example = "ARRAY_INDEX_OUT_OF_BOUNDS")
        MistakeType type,

        @Schema(description = "Human-readable diagnostic explanation", example = "Array index is out of bounds [index 3 >= length 3].")
        String message,

        @Schema(description = "Extracted factual evidence from compiler output, stack trace, or test assertion")
        String evidence,

        @Schema(description = "Mistake severity level", example = "HIGH")
        MistakeSeverity severity,

        @Schema(description = "Diagnostic confidence score (0.0 to 1.0)", example = "1.0")
        double confidence,

        @Schema(description = "Timestamp when the analysis was performed")
        Instant createdAt
) {
    public MistakeAnalysis {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(message, "message must not be null");
        if (severity == null) {
            severity = MistakeSeverity.MEDIUM;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (confidence < 0.0 || confidence > 1.0) {
            confidence = Math.max(0.0, Math.min(1.0, confidence));
        }
    }

    public static MistakeAnalysis of(
            MistakeType type,
            String message,
            String evidence,
            MistakeSeverity severity,
            double confidence
    ) {
        return new MistakeAnalysis(
                type,
                message,
                evidence != null ? evidence : "",
                severity,
                confidence,
                Instant.now()
        );
    }

    public static MistakeAnalysis compileError(String message, String evidence) {
        return of(MistakeType.COMPILE_ERROR, message, evidence, MistakeSeverity.CRITICAL, 1.0);
    }

    public static MistakeAnalysis runtimeError(MistakeType type, String message, String evidence) {
        return of(type != null ? type : MistakeType.RUNTIME_ERROR, message, evidence, MistakeSeverity.HIGH, 1.0);
    }

    public static MistakeAnalysis timeout(String message, String evidence, boolean isInfiniteLoop) {
        return of(isInfiniteLoop ? MistakeType.INFINITE_LOOP : MistakeType.TIME_LIMIT, message, evidence, MistakeSeverity.HIGH, 1.0);
    }

    public static MistakeAnalysis memoryLimit(String message, String evidence) {
        return of(MistakeType.MEMORY_LIMIT, message, evidence, MistakeSeverity.CRITICAL, 1.0);
    }

    public static MistakeAnalysis wrongAnswer(String message, String evidence) {
        return of(MistakeType.INCORRECT_OUTPUT, message, evidence, MistakeSeverity.MEDIUM, 1.0);
    }

    public static MistakeAnalysis none(String evidence) {
        return of(MistakeType.NONE, "All test cases passed. No mistake detected.", evidence, MistakeSeverity.LOW, 1.0);
    }
}
