package com.aicodingmentor.auth.service;

import com.aicodingmentor.auth.dto.ChangePasswordRequest;
import com.aicodingmentor.auth.dto.ForgotPasswordRequest;
import com.aicodingmentor.auth.dto.ForgotPasswordResponse;
import com.aicodingmentor.auth.dto.ResetPasswordRequest;

import java.util.UUID;

public interface PasswordResetService {

    ForgotPasswordResponse requestPasswordReset(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(UUID userId, ChangePasswordRequest request);
}
