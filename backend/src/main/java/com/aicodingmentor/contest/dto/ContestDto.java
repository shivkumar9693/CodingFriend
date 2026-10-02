package com.aicodingmentor.contest.dto;

import com.aicodingmentor.contest.domain.Contest;
import com.aicodingmentor.contest.domain.ContestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Schema(description = "Full contest details response")
public record ContestDto(
        @Schema(description = "Contest UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Contest title", example = "Weekly Algorithm Cup #14")
        String title,

        @Schema(description = "Contest URL slug", example = "weekly-algorithm-cup-14")
        String slug,

        @Schema(description = "Contest description / rules")
        String description,

        @Schema(description = "Scheduled start timestamp")
        Instant startTime,

        @Schema(description = "Scheduled end timestamp")
        Instant endTime,

        @Schema(description = "Duration in minutes", example = "90")
        int durationMinutes,

        @Schema(description = "Contest state: UPCOMING, LIVE, ENDED, ARCHIVED", example = "LIVE")
        ContestStatus status,

        @Schema(description = "Total registered participant count", example = "128")
        long participantCount,

        @Schema(description = "Whether the requesting user is registered for this contest", example = "true")
        boolean isRegistered,

        @Schema(description = "List of contest problems in sequential order")
        List<ContestProblemDto> problems,

        @Schema(description = "Contest creation timestamp")
        Instant createdAt
) {
    public static ContestDto from(Contest contest, long participantCount, boolean isRegistered) {
        if (contest == null) {
            return null;
        }
        List<ContestProblemDto> problemDtos = contest.getProblems() != null
                ? contest.getProblems().stream().map(ContestProblemDto::from).collect(Collectors.toList())
                : List.of();

        return new ContestDto(
                contest.getId(),
                contest.getTitle(),
                contest.getSlug(),
                contest.getDescription(),
                contest.getStartTime(),
                contest.getEndTime(),
                contest.getDurationMinutes(),
                contest.calculateStatus(Instant.now()),
                participantCount,
                isRegistered,
                problemDtos,
                contest.getCreatedAt()
        );
    }
}
