package com.aicodingmentor.submission.service;

import com.aicodingmentor.common.api.PageResponse;
import com.aicodingmentor.submission.dto.CreateSubmissionRequest;
import com.aicodingmentor.submission.dto.SubmissionResponse;
import com.aicodingmentor.submission.dto.SubmissionSummaryResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SubmissionService {

    SubmissionResponse createSubmission(UUID userId, CreateSubmissionRequest request);

    SubmissionResponse getSubmissionById(UUID submissionId, UUID requestingUserId, boolean isAdmin);

    PageResponse<SubmissionSummaryResponse> getSubmissions(
            UUID requestingUserId,
            boolean isAdmin,
            UUID filterUserId,
            UUID filterProblemId,
            Pageable pageable
    );
}
