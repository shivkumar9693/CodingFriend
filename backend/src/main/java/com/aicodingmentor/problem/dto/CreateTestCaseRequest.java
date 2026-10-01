package com.aicodingmentor.problem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Test case creation / update payload")
public record CreateTestCaseRequest(
        @Schema(description = "Input data for the test case", example = "[2, 7, 11, 15]\n9")
        @NotBlank(message = "Input data cannot be blank")
        String inputData,

        @Schema(description = "Expected output for the test case", example = "[0, 1]")
        @NotBlank(message = "Expected output cannot be blank")
        String expectedOutput,

        @Schema(description = "Whether the test case is hidden during grading", example = "false")
        boolean isHidden,

        @Schema(description = "Execution order index", example = "1")
        int sequenceOrder,

        @Schema(description = "Explanation for sample test cases", example = "nums[0] + nums[1] = 9")
        String explanation,

        @Schema(description = "Optional metadata or hint context in JSON/text format", example = "{\"category\":\"edge_case\"}")
        String metadata
) {
    public CreateTestCaseRequest(String inputData, String expectedOutput, boolean isHidden, int sequenceOrder, String explanation) {
        this(inputData, expectedOutput, isHidden, sequenceOrder, explanation, null);
    }
}
