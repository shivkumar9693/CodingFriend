package com.aicodingmentor.learning.dto;

import com.aicodingmentor.learning.domain.MasteryStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload to update a saved revision note")
public record UpdateRevisionNoteRequest(
        @Schema(description = "User's updated reflection or mnemonic notes")
        String userReflections,

        @Schema(description = "Mastery lifecycle status")
        MasteryStatus masteryStatus,

        @Schema(description = "Bookmark flag")
        Boolean isBookmarked
) {
}
