package com.aicodingmentor.problem.dto;

import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Problem list summary response")
public record ProblemSummaryResponse(
        @Schema(description = "Problem identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Problem title", example = "Two Sum")
        String title,

        @Schema(description = "URL-friendly problem slug", example = "two-sum")
        String slug,

        @Schema(description = "Difficulty level", example = "EASY")
        ProblemDifficulty difficulty,

        @Schema(description = "Supported programming language", example = "JAVA")
        String supportedLanguage,

        @Schema(description = "Algorithm and data structure tags", example = "[\"ARRAY\", \"HASH_TABLE\"]")
        Set<String> tags,

        @Schema(description = "Timestamp when problem was created")
        Instant createdAt,

        @Schema(description = "Timestamp when problem was last updated")
        Instant updatedAt
) {
    public static ProblemSummaryResponse from(Problem problem) {
        if (problem == null) {
            return null;
        }
        return new ProblemSummaryResponse(
                problem.getId(),
                problem.getTitle(),
                problem.getSlug(),
                problem.getDifficulty(),
                problem.getSupportedLanguage(),
                problem.getTags(),
                problem.getCreatedAt(),
                problem.getUpdatedAt()
        );
    }
}
