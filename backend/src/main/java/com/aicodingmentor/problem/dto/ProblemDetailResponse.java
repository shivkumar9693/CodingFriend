package com.aicodingmentor.problem.dto;

import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Detailed problem response for learners (hidden test cases excluded)")
public record ProblemDetailResponse(
        @Schema(description = "Problem identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Problem title", example = "Two Sum")
        String title,

        @Schema(description = "URL-friendly problem slug", example = "two-sum")
        String slug,

        @Schema(description = "Detailed problem description in markdown", example = "Given an array of integers nums and an integer target...")
        String description,

        @Schema(description = "Difficulty level", example = "EASY")
        ProblemDifficulty difficulty,

        @Schema(description = "Problem constraints", example = "2 <= nums.length <= 10^4\n-10^9 <= nums[i] <= 10^9")
        String constraints,

        @Schema(description = "Specification for input format", example = "First line contains array nums, second line contains integer target")
        String inputFormat,

        @Schema(description = "Specification for output format", example = "Indices of the two numbers such that they add up to target")
        String outputFormat,

        @Schema(description = "Example inputs and outputs in markdown/text format", example = "Input: nums = [2,7,11,15], target = 9\nOutput: [0,1]")
        String examples,

        @Schema(description = "Starter boilerplate code for the learner", example = "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        \n    }\n}")
        String starterCode,

        @Schema(description = "Primary supported programming language", example = "JAVA")
        String supportedLanguage,

        @Schema(description = "All available supported programming languages for this problem")
        List<String> supportedLanguages,

        @Schema(description = "Starter template code mapped by language key (JAVA, PYTHON, CPP, JAVASCRIPT)")
        Map<String, String> starterTemplates,

        @Schema(description = "Problem category tags", example = "[\"ARRAY\", \"HASH_TABLE\"]")
        Set<String> tags,

        @Schema(description = "Public sample test cases visible to the learner")
        List<TestCaseResponse> sampleTestCases,

        @Schema(description = "Timestamp when problem was created")
        Instant createdAt,

        @Schema(description = "Timestamp when problem was last updated")
        Instant updatedAt
) {
    public static final List<String> ALL_SUPPORTED_LANGUAGES = List.of("JAVA", "PYTHON", "CPP", "JAVASCRIPT");

    public static ProblemDetailResponse from(Problem problem, List<TestCaseResponse> sampleTestCases) {
        return from(problem, sampleTestCases, Map.of("JAVA", problem.getStarterCode() != null ? problem.getStarterCode() : ""));
    }

    public static ProblemDetailResponse from(
            Problem problem,
            List<TestCaseResponse> sampleTestCases,
            Map<String, String> starterTemplates
    ) {
        if (problem == null) {
            return null;
        }
        return new ProblemDetailResponse(
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
                ALL_SUPPORTED_LANGUAGES,
                starterTemplates != null ? starterTemplates : Map.of(),
                problem.getTags() != null ? Set.copyOf(problem.getTags()) : Set.of(),
                sampleTestCases != null ? sampleTestCases : List.of(),
                problem.getCreatedAt(),
                problem.getUpdatedAt()
        );
    }
}
