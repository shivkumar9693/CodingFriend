package com.aicodingmentor.mentor.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Progressive Socratic hint tiers for SocraBuddy AI mentor")
public enum HintLevel {
    @Schema(description = "Level 1: Gentle observational question to spark critical thinking")
    LEVEL_1_NUDGE,

    @Schema(description = "Level 2: Conceptual clue explaining the underlying algorithm or data structure property")
    LEVEL_2_CONCEPT,

    @Schema(description = "Level 3: Algorithmic blueprint detailing step-by-step logic flow")
    LEVEL_3_ALGORITHM,

    @Schema(description = "Level 4: Targeted pointer highlighting the specific construct or condition with the bug")
    LEVEL_4_TARGETED
}
