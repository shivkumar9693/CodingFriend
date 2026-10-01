package com.aicodingmentor.problem.exception;

import com.aicodingmentor.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class ProblemNotFoundException extends ApiException {
    public ProblemNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
