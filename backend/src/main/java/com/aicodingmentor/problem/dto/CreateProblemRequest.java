package com.aicodingmentor.problem.dto;

import com.aicodingmentor.problem.domain.ProblemDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

@Schema(description = "Request payload for creating a new coding problem")
public record CreateProblemRequest(
        @Schema(description = "Problem title", example = "Two Sum")
        @NotBlank(message = "Title cannot be blank")
        @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
        String title,

        @Schema(description = "Custom URL slug (optional, auto-generated from title if omitted)", example = "two-sum")
        @Size(max = 255, message = "Slug cannot exceed 255 characters")
        String slug,

        @Schema(description = "Full problem statement in markdown", example = "Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.")
        @NotBlank(message = "Description cannot be blank")
        String description,

        @Schema(description = "Difficulty level", example = "EASY")
        @NotNull(message = "Difficulty is required")
        ProblemDifficulty difficulty,

        @Schema(description = "Problem constraints", example = "2 <= nums.length <= 10^4\n-10^9 <= nums[i] <= 10^9")
        String constraints,

        @Schema(description = "Input format specification", example = "Line 1: array nums\nLine 2: target integer")
        String inputFormat,

        @Schema(description = "Output format specification", example = "Array of 2 indices [i, j]")
        String outputFormat,

        @Schema(description = "Example inputs and outputs in markdown", example = "Example 1:\nInput: nums = [2,7,11,15], target = 9\nOutput: [0,1]")
        String examples,

        @Schema(description = "Starter template code for Java learners", example = "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        \n    }\n}")
        String starterCode,

        @Schema(description = "Supported programming language", example = "JAVA")
        String supportedLanguage,

        @Schema(description = "Algorithm/Data structure tags", example = "[\"ARRAY\", \"HASH_TABLE\"]")
        Set<String> tags,

        @Schema(description = "Initial test cases (both public sample and hidden grading test cases)")
        @Valid
        List<CreateTestCaseRequest> testCases
) {
}
