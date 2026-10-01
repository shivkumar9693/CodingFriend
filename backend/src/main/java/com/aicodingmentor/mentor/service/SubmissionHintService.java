package com.aicodingmentor.mentor.service;

import com.aicodingmentor.mentor.dto.SubmissionHintRequest;
import com.aicodingmentor.mentor.dto.SubmissionHintResponse;

import java.util.UUID;

/**
 * Service contract for generating, validating, and persisting tiered Socratic hints on submissions.
 */
public interface SubmissionHintService {

    /**
     * Generates a tiered Socratic hint for a specific submission attempt.
     * Enforces hint progression constraints and prevents premature solution exposure.
     *
     * @param userId requesting user UUID
     * @param submissionId target submission UUID
     * @param request requested hint level
     * @return structured hint response
     */
    SubmissionHintResponse generateSubmissionHint(UUID userId, UUID submissionId, SubmissionHintRequest request);
}
