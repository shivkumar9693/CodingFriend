package com.aicodingmentor.execution.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evaluation verdict for code execution and problem grading")
public enum EvaluationVerdict {
    @Schema(description = "All test cases passed successfully")
    ACCEPTED,

    @Schema(description = "Output does not match expected output")
    WRONG_ANSWER,

    @Schema(description = "Source code failed to compile")
    COMPILE_ERROR,

    @Schema(description = "Program crashed with runtime exception or non-zero exit code")
    RUNTIME_ERROR,

    @Schema(description = "Program exceeded time limit")
    TIME_LIMIT_EXCEEDED,

    @Schema(description = "Program exceeded memory limit")
    MEMORY_LIMIT_EXCEEDED
}
