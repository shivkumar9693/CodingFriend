package com.aicodingmentor.auth.dto;

import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.domain.UserRole;

import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String fullName,
        UserRole role
) {
    public static UserDto fromUser(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }
}
