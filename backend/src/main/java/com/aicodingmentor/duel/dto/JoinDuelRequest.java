package com.aicodingmentor.duel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to join an existing 1v1 Code Duel room via room code")
public record JoinDuelRequest(
        @NotBlank(message = "Room code is required")
        @Schema(description = "6-character room code", example = "DUEL-9K2A")
        String roomCode
) {
    public record DuelProgressUpdate(
            @Min(0) @Max(100)
            int progressPercent,
            String lastAction
    ) {}
}
