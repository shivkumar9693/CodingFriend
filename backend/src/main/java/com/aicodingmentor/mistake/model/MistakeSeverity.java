package com.aicodingmentor.mistake.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Severity level of an identified mistake")
public enum MistakeSeverity {
    @Schema(description = "Low severity - minor formatting or informational")
    LOW,

    @Schema(description = "Medium severity - logical bug or incorrect output on test case")
    MEDIUM,

    @Schema(description = "High severity - runtime exception or timeout")
    HIGH,

    @Schema(description = "Critical severity - compilation error or memory exhaustion")
    CRITICAL
}
