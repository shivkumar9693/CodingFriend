package com.aicodingmentor.contest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Contest live standings and leaderboard")
public record ContestLeaderboardDto(
        @Schema(description = "Contest UUID")
        UUID contestId,

        @Schema(description = "Contest title")
        String title,

        @Schema(description = "Contest status")
        String status,

        @Schema(description = "Total number of participants")
        int totalParticipants,

        @Schema(description = "Ordered list of participants by score and penalty")
        List<ContestParticipantDto> standings
) {}
