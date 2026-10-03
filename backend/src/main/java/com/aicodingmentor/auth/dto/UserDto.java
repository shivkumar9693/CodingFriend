package com.aicodingmentor.auth.dto;

import com.aicodingmentor.user.domain.AuthProvider;
import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.domain.UserRole;

import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String fullName,
        UserRole role,
        String avatarUrl,
        AuthProvider authProvider
) {
    public UserDto(UUID id, String email, String fullName, UserRole role) {
        this(id, email, fullName, role, null, AuthProvider.LOCAL);
    }

    public static UserDto fromUser(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getAvatarUrl(),
                user.getAuthProvider()
        );
    }
}
