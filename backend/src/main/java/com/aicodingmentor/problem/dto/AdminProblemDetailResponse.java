package com.aicodingmentor.problem.dto;

import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Full problem response for administrators including all test cases")
public record AdminProblemDetailResponse(
        @Schema(description = "Problem identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Problem title", example = "Two Sum")
        String title,

        @Schema(description = "URL-friendly problem slug", example = "two-sum")
        String slug,

        @Schema(description = "Problem description in markdown", example = "Given an array of integers nums...")
        String description,

        @Schema(description = "Difficulty level", example = "EASY")
        ProblemDifficulty difficulty,

        @Schema(description = "Problem constraints", example = "2 <= nums.length <= 10^4")
        String constraints,

        @Schema(description = "Input format specification", example = "nums = [2,7,11,15], target = 9")
        String inputFormat,

        @Schema(description = "Output format specification", example = "[0,1]")
        String outputFormat,

        @Schema(description = "Example inputs and outputs in markdown", example = "Input: nums = [2,7,11,15], target = 9...")
        String examples,

        @Schema(description = "Starter boilerplate code", example = "class Solution { ... }")
        String starterCode,

        @Schema(description = "Supported programming language", example = "JAVA")
        String supportedLanguage,

        @Schema(description = "Problem tags", example = "[\"ARRAY\", \"HASH_TABLE\"]")
        Set<String> tags,

        @Schema(description = "All test cases including hidden grading suites")
        List<TestCaseResponse> testCases,

        @Schema(description = "Creation timestamp")
        Instant createdAt,

        @Schema(description = "Last update timestamp")
        Instant updatedAt
) {
    public static AdminProblemDetailResponse from(Problem problem, List<TestCaseResponse> allTestCases) {
        if (problem == null) {
            return null;
        }
        return new AdminProblemDetailResponse(
                problem.getId(),
                problem.getTitle(),
                problem.getSlug(),
                problem.getDescription(),
                problem.getDifficulty(),
                problem.getConstraints(),
                problem.getInputFormat(),
                problem.getOutputFormat(),
                problem.getExamples(),
                problem.getStarterCode(),
                problem.getSupportedLanguage(),
                problem.getTags(),
                allTestCases != null ? allTestCases : List.of(),
                problem.getCreatedAt(),
                problem.getUpdatedAt()
        );
    }
}
