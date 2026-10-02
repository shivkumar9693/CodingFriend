package com.aicodingmentor.contest.dto;

import com.aicodingmentor.contest.domain.ContestParticipant;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Contest participant rank and score telemetry")
public record ContestParticipantDto(
        @Schema(description = "Participant record UUID")
        UUID id,

        @Schema(description = "User identifier")
        UUID userId,

        @Schema(description = "Participant full name", example = "Alex Mercer")
        String fullName,

        @Schema(description = "Participant avatar URL")
        String avatarUrl,

        @Schema(description = "Current rank in contest", example = "1")
        Integer rank,

        @Schema(description = "Total points accumulated", example = "1800")
        int totalScore,

        @Schema(description = "Total penalty time in minutes", example = "45")
        int totalPenaltyMinutes,

        @Schema(description = "Participant contest ELO rating before contest", example = "1420")
        Integer ratingBefore,

        @Schema(description = "Participant contest ELO rating after contest", example = "1465")
        Integer ratingAfter,

        @Schema(description = "Number of tab switches or proctoring warnings", example = "0")
        int tabSwitchCount,

        @Schema(description = "Timestamp when user registered")
        Instant registeredAt
) {
    public static ContestParticipantDto from(ContestParticipant p, int calculatedRank) {
        if (p == null) {
            return null;
        }
        Integer rank = calculatedRank > 0 ? Integer.valueOf(calculatedRank) : p.getFinalRank();
        return new ContestParticipantDto(
                p.getId(),
                p.getUser() != null ? p.getUser().getId() : null,
                p.getUser() != null ? p.getUser().getFullName() : "Anonymous",
                p.getUser() != null ? p.getUser().getAvatarUrl() : null,
                rank,
                p.getTotalScore(),
                p.getTotalPenaltyMinutes(),
                p.getRatingBefore(),
                p.getRatingAfter(),
                p.getTabSwitchCount(),
                p.getRegisteredAt()
        );
    }
}
