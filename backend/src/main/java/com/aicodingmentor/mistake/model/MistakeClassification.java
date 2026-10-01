package com.aicodingmentor.mistake.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Classification of learner programming mistakes")
public enum MistakeClassification {
    SYNTAX_ERROR,
    OFF_BY_ONE,
    NULL_POINTER,
    INDEX_OUT_OF_BOUNDS,
    INFINITE_LOOP,
    TIME_LIMIT_EXCEEDED,
    OUTPUT_FORMAT_MISMATCH,
    LOGIC_INCORRECT,
    EDGE_CASE_MISSED,
    GENERAL_BUG
}
