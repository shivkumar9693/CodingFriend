package com.aicodingmentor.duel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

@Schema(description = "Request to create or queue for a 1v1 Code Duel")
public record CreateDuelRequest(
        @NotBlank(message = "Difficulty is required (EASY, MEDIUM, HARD)")
        @Pattern(regexp = "^(EASY|MEDIUM|HARD)$", message = "Difficulty must be EASY, MEDIUM, or HARD")
        @Schema(description = "Difficulty level", example = "EASY")
        String difficulty,

        @Schema(description = "Optional specific problem ID for custom challenge")
        UUID problemId
) {}
