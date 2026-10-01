package com.aicodingmentor.problem.exception;

import com.aicodingmentor.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class DuplicateProblemSlugException extends ApiException {
    public DuplicateProblemSlugException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
