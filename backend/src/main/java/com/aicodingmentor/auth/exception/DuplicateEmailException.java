package com.aicodingmentor.auth.exception;

import com.aicodingmentor.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends ApiException {
    public DuplicateEmailException(String message) {
        super(HttpStatus.CONFLICT, message);
    }

    public static DuplicateEmailException forEmail(String email) {
        return new DuplicateEmailException("Email is already registered: " + email);
    }
}
