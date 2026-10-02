package com.aicodingmentor.duel.dto;

import com.aicodingmentor.duel.domain.Duel;
import com.aicodingmentor.duel.domain.DuelStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "1v1 Code Duel room details")
public record DuelDto(
        @Schema(description = "Duel UUID")
        UUID id,

        @Schema(description = "6-character room invite code", example = "DUEL-9K2A")
        String roomCode,

        @Schema(description = "Player 1 ID")
        UUID player1Id,

        @Schema(description = "Player 1 Full Name", example = "Alex Mercer")
        String player1Name,

        @Schema(description = "Player 1 Duel ELO Rating", example = "1350")
        Integer player1Rating,

        @Schema(description = "Player 2 ID")
        UUID player2Id,

        @Schema(description = "Player 2 Full Name", example = "Jordan Lee")
        String player2Name,

        @Schema(description = "Player 2 Duel ELO Rating", example = "1375")
        Integer player2Rating,

        @Schema(description = "Problem UUID")
        UUID problemId,

        @Schema(description = "Problem Title", example = "Two Sum")
        String problemTitle,

        @Schema(description = "Problem Slug", example = "two-sum")
        String problemSlug,

        @Schema(description = "Selected Difficulty", example = "EASY")
        String difficulty,

        @Schema(description = "Duel Status: WAITING, IN_PROGRESS, COMPLETED, CANCELLED", example = "IN_PROGRESS")
        DuelStatus status,

        @Schema(description = "Winner User ID")
        UUID winnerId,

        @Schema(description = "Winner Name")
        String winnerName,

        @Schema(description = "Match start timestamp")
        Instant startTime,

        @Schema(description = "Match finish timestamp")
        Instant endTime,

        @Schema(description = "Player 1 percentage/testcase progress (0-100)", example = "75")
        int player1Progress,

        @Schema(description = "Player 2 percentage/testcase progress (0-100)", example = "50")
        int player2Progress,

        @Schema(description = "Room creation timestamp")
        Instant createdAt
) {
    public static DuelDto from(Duel duel) {
        if (duel == null) {
            return null;
        }
        return new DuelDto(
                duel.getId(),
                duel.getRoomCode(),
                duel.getPlayer1() != null ? duel.getPlayer1().getId() : null,
                duel.getPlayer1() != null ? duel.getPlayer1().getFullName() : "Anonymous",
                duel.getPlayer1() != null ? duel.getPlayer1().getDuelRating() : 1200,
                duel.getPlayer2() != null ? duel.getPlayer2().getId() : null,
                duel.getPlayer2() != null ? duel.getPlayer2().getFullName() : null,
                duel.getPlayer2() != null ? duel.getPlayer2().getDuelRating() : null,
                duel.getProblem() != null ? duel.getProblem().getId() : null,
                duel.getProblem() != null ? duel.getProblem().getTitle() : null,
                duel.getProblem() != null ? duel.getProblem().getSlug() : null,
                duel.getDifficulty(),
                duel.getStatus(),
                duel.getWinner() != null ? duel.getWinner().getId() : null,
                duel.getWinner() != null ? duel.getWinner().getFullName() : null,
                duel.getStartTime(),
                duel.getEndTime(),
                duel.getPlayer1Progress(),
                duel.getPlayer2Progress(),
                duel.getCreatedAt()
        );
    }
}
