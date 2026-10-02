package com.aicodingmentor.mentor.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Socratic inline micro-nudge response for editor ghost widget")
public record InlineAssistResponse(
        @Schema(description = "Type of inline nudge (e.g., LOGIC_QUESTION, SYNTAX_POINTER, EDGE_CASE_CHECK)", example = "LOGIC_QUESTION")
        String nudgeType,

        @Schema(description = "Short 1-2 sentence Socratic prompt / question to guide the next line", example = "What data structure can help you check complement in O(1) time here?")
        String inlineMessage,

        @Schema(description = "Line number targeted by the nudge", example = "5")
        int targetLine,

        @Schema(description = "Whether the response reveals the solution (always false)", example = "false")
        boolean revealsSolution,

        @Schema(description = "AI Model provider", example = "qwen2.5-coder:latest")
        String modelName
) {
    public static InlineAssistResponse of(String nudgeType, String inlineMessage, int targetLine, String modelName) {
        return new InlineAssistResponse(nudgeType, inlineMessage, targetLine, false, modelName);
    }
}
