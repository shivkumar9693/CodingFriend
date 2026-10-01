package com.aicodingmentor.mistake.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Deterministic and AI-assisted mistake classifications")
public enum MistakeType {
    @Schema(description = "Java/Language syntax, type, or compiler errors")
    COMPILE_ERROR,

    @Schema(description = "Array or list index out of bounds")
    ARRAY_INDEX_OUT_OF_BOUNDS,

    @Schema(description = "Null pointer dereference")
    NULL_POINTER,

    @Schema(description = "Failed to parse string into numeric type")
    NUMBER_FORMAT,

    @Schema(description = "Division or modulo by zero")
    ARITHMETIC_EXCEPTION,

    @Schema(description = "Uncontrolled recursion stack overflow")
    STACK_OVERFLOW,

    @Schema(description = "Generic unhandled runtime exception")
    RUNTIME_ERROR,

    @Schema(description = "Execution exceeded time limit or infinite loop")
    TIME_LIMIT,

    @Schema(description = "Detected infinite loop structure")
    INFINITE_LOOP,

    @Schema(description = "Memory cap exceeded (heap exhaustion)")
    MEMORY_LIMIT,

    @Schema(description = "Output does not match expected test case output")
    WRONG_ANSWER,

    @Schema(description = "Specific mismatch in actual vs expected values")
    INCORRECT_OUTPUT,

    // --- Day 16: AI-Assisted Logical Mistake Categories ---
    @Schema(description = "Loop boundary condition off by one index")
    OFF_BY_ONE,

    @Schema(description = "Incorrect boolean or conditional check")
    WRONG_CONDITION,

    @Schema(description = "Missed boundary or edge case scenario")
    EDGE_CASE,

    @Schema(description = "Suboptimal algorithmic time or space complexity")
    INEFFICIENT_ALGORITHM,

    @Schema(description = "Suboptimal or incorrect data structure choice")
    INCORRECT_DATA_STRUCTURE,

    @Schema(description = "Faulty base case or recursion step logic")
    RECURSION_ERROR,

    @Schema(description = "General algorithmic logic error")
    LOGICAL_ERROR,

    @Schema(description = "Other unclassified mistake")
    OTHER,

    @Schema(description = "No mistake detected - all tests passed")
    NONE,

    @Schema(description = "Unknown or low-confidence failure")
    UNKNOWN
}
