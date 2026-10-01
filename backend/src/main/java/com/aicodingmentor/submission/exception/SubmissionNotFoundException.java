package com.aicodingmentor.submission.exception;

import com.aicodingmentor.common.error.ResourceNotFoundException;

import java.util.UUID;

public class SubmissionNotFoundException extends ResourceNotFoundException {

    public SubmissionNotFoundException(UUID submissionId) {
        super("Submission", submissionId);
    }
}
