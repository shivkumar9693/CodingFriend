package com.aicodingmentor.submission.exception;

import com.aicodingmentor.common.error.ApiException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class SubmissionAccessDeniedException extends ApiException {

    public SubmissionAccessDeniedException(UUID submissionId) {
        super(HttpStatus.FORBIDDEN, String.format("You do not have permission to view submission: '%s'", submissionId));
    }

    public SubmissionAccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
