package com.aicodingmentor.auth.service;

import com.aicodingmentor.auth.dto.AuthResponse;
import com.aicodingmentor.auth.dto.GoogleLoginRequest;
import com.aicodingmentor.auth.dto.LoginRequest;
import com.aicodingmentor.auth.dto.LogoutRequest;
import com.aicodingmentor.auth.dto.RefreshTokenRequest;
import com.aicodingmentor.auth.dto.RegisterRequest;
import com.aicodingmentor.auth.dto.TokenRefreshResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse loginWithGoogle(GoogleLoginRequest request);

    TokenRefreshResponse refreshToken(RefreshTokenRequest request);

    void logout(LogoutRequest request);
}
