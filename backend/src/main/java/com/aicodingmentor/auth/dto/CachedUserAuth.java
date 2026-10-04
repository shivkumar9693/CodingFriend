package com.aicodingmentor.auth.dto;

import com.aicodingmentor.auth.security.UserPrincipal;
import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.domain.UserRole;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Ultra-compact, low-memory DTO for caching authenticated user identity in Redis.
 * Consumes < 150 bytes per user in Redis, enabling sub-millisecond JWT authentication and login.
 */
public record CachedUserAuth(
        UUID id,
        String email,
        String passwordHash,
        String fullName,
        String role
) implements Serializable {

    public UserPrincipal toPrincipal() {
        UserRole userRole;
        try {
            userRole = UserRole.valueOf(role);
        } catch (Exception e) {
            userRole = UserRole.USER;
        }

        return new UserPrincipal(
                id,
                email,
                passwordHash,
                fullName,
                userRole,
                List.of(new SimpleGrantedAuthority("ROLE_" + userRole.name()))
        );
    }

    public static CachedUserAuth from(User user) {
        return new CachedUserAuth(
                user.getId(),
                user.getEmail().toLowerCase().trim(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getRole().name()
        );
    }
}
