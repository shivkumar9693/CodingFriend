package com.aicodingmentor.execution.model;

public enum ExecutionFailureReason {
    COMPILATION_ERROR,
    RUNTIME_ERROR,
    TIME_LIMIT_EXCEEDED,
    MEMORY_LIMIT_EXCEEDED,
    OUTPUT_LIMIT_EXCEEDED,
    INVALID_SOURCE,
    SECURITY_VIOLATION,
    SYSTEM_ERROR
}
