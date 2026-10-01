package com.aicodingmentor.mentor.dto;

import com.aicodingmentor.mentor.model.HintLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Socratic guidance response from SocraBuddy AI mentor")
public record SocraBuddyHintResponse(
        @Schema(description = "Socratic hint tier delivered", example = "LEVEL_1_NUDGE")
        HintLevel hintLevel,

        @Schema(description = "Engaging title for the hint", example = "Observing the Boundary Condition")
        String title,

        @Schema(description = "Socratic explanation guiding the learner without writing solution code")
        String guidance,

        @Schema(description = "Thought-provoking question to prompt learner self-discovery", example = "What happens when the array contains only one element?")
        String socraticQuestion,

        @Schema(description = "Core algorithmic concepts highlighted in this hint", example = "[\"Two Pointers\", \"Boundary Condition\"]")
        List<String> conceptKeywords,

        @Schema(description = "Model or engine that generated this hint", example = "qwen2.5-coder:latest")
        String modelUsed
) {
    public static SocraBuddyHintResponse of(
            HintLevel level,
            String title,
            String guidance,
            String socraticQuestion,
            List<String> keywords,
            String model
    ) {
        return new SocraBuddyHintResponse(
                level,
                title,
                guidance,
                socraticQuestion,
                keywords != null ? keywords : List.of(),
                model != null ? model : "SocraBuddy Engine"
        );
    }
}
