package com.aicodingmentor.contest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Admin payload to create or schedule a new contest")
public record CreateContestRequest(
        @NotBlank(message = "Contest title is required")
        @Size(max = 255, message = "Contest title must not exceed 255 characters")
        @Schema(description = "Contest Title", example = "Spring Coding Championship 2026")
        String title,

        @Schema(description = "Contest description and rules")
        String description,

        @NotNull(message = "Start time is required")
        @Future(message = "Contest start time must be in the future")
        @Schema(description = "Contest scheduled start timestamp")
        Instant startTime,

        @Min(value = 15, message = "Duration must be at least 15 minutes")
        @Schema(description = "Contest duration in minutes", example = "90")
        int durationMinutes,

        @NotEmpty(message = "Contest must have at least one problem")
        @Schema(description = "List of problem IDs and point weights to include")
        List<ContestProblemSetup> problems
) {
    public record ContestProblemSetup(
            @NotNull(message = "Problem ID is required")
            UUID problemId,

            @Min(value = 1, message = "Problem order must be positive")
            int problemOrder,

            @Min(value = 50, message = "Points must be at least 50")
            int points
    ) {}
}
