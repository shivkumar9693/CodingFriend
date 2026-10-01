package com.aicodingmentor.problem.exception;

import com.aicodingmentor.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class TestCaseNotFoundException extends ApiException {
    public TestCaseNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
