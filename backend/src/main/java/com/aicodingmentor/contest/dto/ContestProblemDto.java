package com.aicodingmentor.contest.dto;

import com.aicodingmentor.contest.domain.ContestProblem;
import com.aicodingmentor.problem.domain.Problem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Contest problem mapping details")
public record ContestProblemDto(
        @Schema(description = "Problem UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID problemId,

        @Schema(description = "Problem title", example = "Two Sum")
        String title,

        @Schema(description = "Problem URL slug", example = "two-sum")
        String slug,

        @Schema(description = "Difficulty level", example = "EASY")
        String difficulty,

        @Schema(description = "Order in contest", example = "1")
        int problemOrder,

        @Schema(description = "Score points allocated for solving this problem", example = "500")
        int points
) {
    public static ContestProblemDto from(ContestProblem cp) {
        if (cp == null || cp.getProblem() == null) {
            return null;
        }
        Problem p = cp.getProblem();
        return new ContestProblemDto(
                p.getId(),
                p.getTitle(),
                p.getSlug(),
                p.getDifficulty() != null ? p.getDifficulty().name() : "EASY",
                cp.getProblemOrder(),
                cp.getPoints()
        );
    }
}
