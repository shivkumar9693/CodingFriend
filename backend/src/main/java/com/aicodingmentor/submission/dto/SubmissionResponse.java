package com.aicodingmentor.submission.dto;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.submission.domain.Submission;
import com.aicodingmentor.submission.domain.SubmissionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Detailed submission response containing full code and execution status")
public record SubmissionResponse(
        @Schema(description = "Unique submission ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID id,

        @Schema(description = "User ID of the submitter", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
        UUID userId,

        @Schema(description = "Target problem ID", example = "8e177368-aba4-43ce-bb1b-32ee2ffa1f80")
        UUID problemId,

        @Schema(description = "Programming language", example = "JAVA")
        String language,

        @Schema(description = "Submitted source code")
        String sourceCode,

        @Schema(description = "Current lifecycle status of the submission", example = "COMPLETED")
        SubmissionStatus status,

        @Schema(description = "Evaluation verdict", example = "ACCEPTED")
        EvaluationVerdict verdict,

        @Schema(description = "Number of test cases passed", example = "5")
        int testsPassed,

        @Schema(description = "Total number of test cases evaluated", example = "5")
        int totalTests,

        @Schema(description = "Total execution time in milliseconds", example = "85")
        long executionTimeMs,

        @Schema(description = "Sanitized evaluation summary / test case details JSON")
        String evaluationDetails,

        @Schema(description = "Error or diagnostic message if submission failed", example = "null")
        String errorMessage,

        @Schema(description = "Timestamp when the submission was created")
        Instant createdAt,

        @Schema(description = "Timestamp when the submission was last updated")
        Instant updatedAt
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static SubmissionResponse from(Submission submission) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getUserId(),
                submission.getProblemId(),
                submission.getLanguage(),
                submission.getSourceCode(),
                submission.getStatus(),
                submission.getVerdict(),
                submission.getTestsPassed(),
                submission.getTotalTests(),
                submission.getExecutionTimeMs(),
                sanitizeEvaluationDetails(submission.getEvaluationDetails()),
                submission.getErrorMessage(),
                submission.getCreatedAt(),
                submission.getUpdatedAt()
        );
    }

    private static String sanitizeEvaluationDetails(String rawDetails) {
        if (rawDetails == null || rawDetails.isBlank()) {
            return rawDetails;
        }
        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(rawDetails);
            if (rootNode.isArray()) {
                ArrayNode arrayNode = (ArrayNode) rootNode;
                for (JsonNode element : arrayNode) {
                    if (element.isObject()) {
                        ObjectNode obj = (ObjectNode) element;
                        boolean isHidden = obj.has("isHidden") && obj.get("isHidden").asBoolean();
                        if (isHidden) {
                            obj.putNull("input");
                            obj.putNull("expectedOutput");
                            obj.putNull("actualOutput");
                            obj.putNull("errorMessage");
                        }
                    }
                }
                return OBJECT_MAPPER.writeValueAsString(arrayNode);
            }
        } catch (Exception ignored) {
        }
        return rawDetails;
    }
}
