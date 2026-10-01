package com.aicodingmentor.submission.dto;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.submission.domain.Submission;
import com.aicodingmentor.submission.domain.SubmissionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Compact submission summary for paginated lists and learner history")
public record SubmissionSummaryResponse(
        @Schema(description = "Unique submission ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID id,

        @Schema(description = "User ID of the submitter", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
        UUID userId,

        @Schema(description = "Target problem ID", example = "8e177368-aba4-43ce-bb1b-32ee2ffa1f80")
        UUID problemId,

        @Schema(description = "Programming language", example = "JAVA")
        String language,

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

        @Schema(description = "Timestamp when the submission was created")
        Instant createdAt,

        @Schema(description = "Timestamp when the submission was last updated")
        Instant updatedAt
) {
    public static SubmissionSummaryResponse from(Submission submission) {
        return new SubmissionSummaryResponse(
                submission.getId(),
                submission.getUserId(),
                submission.getProblemId(),
                submission.getLanguage(),
                submission.getStatus(),
                submission.getVerdict(),
                submission.getTestsPassed(),
                submission.getTotalTests(),
                submission.getExecutionTimeMs(),
                submission.getCreatedAt(),
                submission.getUpdatedAt()
        );
    }
}
