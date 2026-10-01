package com.aicodingmentor.problem.dto;

import com.aicodingmentor.problem.domain.TestCase;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Test case details")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TestCaseResponse(
        @Schema(description = "Test case identifier", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
        UUID id,

        @Schema(description = "Input data passed to the solution function", example = "[2, 7, 11, 15]\n9")
        String inputData,

        @Schema(description = "Expected output returned by the solution", example = "[0, 1]")
        String expectedOutput,

        @Schema(description = "Whether this test case is hidden from learner views", example = "false")
        Boolean isHidden,

        @Schema(description = "Execution order index", example = "1")
        int sequenceOrder,

        @Schema(description = "Explanation for sample test cases", example = "Because nums[0] + nums[1] == 9, we return [0, 1].")
        String explanation,

        @Schema(description = "Optional structured metadata or diagnostic hints in JSON/text", example = "{\"edgeCase\": \"negative_numbers\"}")
        String metadata
) {
    public static TestCaseResponse from(TestCase testCase, boolean includeHiddenFlag) {
        if (testCase == null) {
            return null;
        }
        return new TestCaseResponse(
                testCase.getId(),
                testCase.getInputData(),
                testCase.getExpectedOutput(),
                includeHiddenFlag ? testCase.isHidden() : null,
                testCase.getSequenceOrder(),
                testCase.getExplanation(),
                includeHiddenFlag ? testCase.getMetadata() : null
        );
    }
}
