package com.aicodingmentor.mentor.dto;

import com.aicodingmentor.mentor.model.HintLevel;
import com.aicodingmentor.mistake.model.MistakeClassification;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Automated mistake diagnosis and Socratic recommendations from SocraBuddy")
public record SocraBuddyDiagnosisResponse(
        @Schema(description = "Classified mistake type", example = "OFF_BY_ONE")
        MistakeClassification mistakeClassification,

        @Schema(description = "Concise summary of why the code failed", example = "Array index exceeded upper bounds by 1 in the loop termination check.")
        String diagnosisSummary,

        @Schema(description = "Socratic guidance prompting the learner to inspect their logic")
        String socraticGuidance,

        @Schema(description = "Concrete non-spoiler action the learner can take to debug", example = "Trace your loop counter variable `i` on paper for an array of length 3.")
        String recommendedAction,

        @Schema(description = "Recommended next hint tier to explore", example = "LEVEL_2_CONCEPT")
        HintLevel recommendedHintLevel
) {
}
