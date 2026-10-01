package com.aicodingmentor.execution.model;

public record ExecutionRequest(
        String sourceCode,
        String className,
        String stdin,
        long timeoutMs,
        int memoryLimitMb,
        double cpuQuota,
        int maxOutputSizeBytes,
        SupportedLanguage language
) {
    public static final long DEFAULT_TIMEOUT_MS = 5000L;
    public static final int DEFAULT_MEMORY_LIMIT_MB = 256;
    public static final double DEFAULT_CPU_QUOTA = 1.0;
    public static final int DEFAULT_MAX_OUTPUT_SIZE_BYTES = 1024 * 1024; // 1MB

    public ExecutionRequest(
            String sourceCode,
            String className,
            String stdin,
            long timeoutMs,
            int memoryLimitMb,
            double cpuQuota,
            int maxOutputSizeBytes
    ) {
        this(sourceCode, className, stdin, timeoutMs, memoryLimitMb, cpuQuota, maxOutputSizeBytes, SupportedLanguage.JAVA);
    }

    public static ExecutionRequest of(String sourceCode) {
        return new ExecutionRequest(
                sourceCode,
                null,
                "",
                DEFAULT_TIMEOUT_MS,
                DEFAULT_MEMORY_LIMIT_MB,
                DEFAULT_CPU_QUOTA,
                DEFAULT_MAX_OUTPUT_SIZE_BYTES,
                SupportedLanguage.JAVA
        );
    }

    public static ExecutionRequest of(String sourceCode, String stdin) {
        return new ExecutionRequest(
                sourceCode,
                null,
                stdin,
                DEFAULT_TIMEOUT_MS,
                DEFAULT_MEMORY_LIMIT_MB,
                DEFAULT_CPU_QUOTA,
                DEFAULT_MAX_OUTPUT_SIZE_BYTES,
                SupportedLanguage.JAVA
        );
    }

    public static ExecutionRequest of(String sourceCode, String stdin, long timeoutMs) {
        return new ExecutionRequest(
                sourceCode,
                null,
                stdin,
                timeoutMs,
                DEFAULT_MEMORY_LIMIT_MB,
                DEFAULT_CPU_QUOTA,
                DEFAULT_MAX_OUTPUT_SIZE_BYTES,
                SupportedLanguage.JAVA
        );
    }

    public static ExecutionRequest of(String sourceCode, String stdin, SupportedLanguage language) {
        return new ExecutionRequest(
                sourceCode,
                null,
                stdin,
                DEFAULT_TIMEOUT_MS,
                DEFAULT_MEMORY_LIMIT_MB,
                DEFAULT_CPU_QUOTA,
                DEFAULT_MAX_OUTPUT_SIZE_BYTES,
                language != null ? language : SupportedLanguage.JAVA
        );
    }
}
