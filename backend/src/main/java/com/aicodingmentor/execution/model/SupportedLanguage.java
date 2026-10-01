package com.aicodingmentor.execution.model;

import java.util.Arrays;

public enum SupportedLanguage {
    JAVA("Java 17", "java", ".java"),
    PYTHON("Python 3", "python", ".py"),
    CPP("C++ 20", "cpp", ".cpp"),
    JAVASCRIPT("JavaScript", "javascript", ".js");

    private final String displayName;
    private final String monacoId;
    private final String fileExtension;

    SupportedLanguage(String displayName, String monacoId, String fileExtension) {
        this.displayName = displayName;
        this.monacoId = monacoId;
        this.fileExtension = fileExtension;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMonacoId() {
        return monacoId;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public static SupportedLanguage fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return JAVA;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "PYTHON", "PYTHON3", "PY" -> PYTHON;
            case "CPP", "C++", "CXX", "CPLUSPLUS" -> CPP;
            case "JAVASCRIPT", "JS", "NODE", "NODEJS" -> JAVASCRIPT;
            case "JAVA", "JAVA17" -> JAVA;
            default -> {
                for (SupportedLanguage lang : values()) {
                    if (lang.name().equalsIgnoreCase(normalized)) {
                        yield lang;
                    }
                }
                yield JAVA;
            }
        };
    }
}
