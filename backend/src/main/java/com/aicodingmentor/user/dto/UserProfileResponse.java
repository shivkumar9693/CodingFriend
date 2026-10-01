package com.aicodingmentor.user.dto;

import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "User profile details response")
public record UserProfileResponse(
        @Schema(description = "Unique user identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Registered email address", example = "learner@example.com")
        String email,

        @Schema(description = "User's display / full name", example = "Alex Mercer")
        String fullName,

        @Schema(description = "User bio or learning aspirations", example = "Aspiring Java backend developer")
        String bio,

        @Schema(description = "GitHub username for coding profile link", example = "alexmercer-dev")
        String githubUsername,

        @Schema(description = "Assigned user role", example = "USER")
        UserRole role,

        @Schema(description = "Account creation timestamp")
        Instant createdAt,

        @Schema(description = "Last profile update timestamp")
        Instant updatedAt
) {
    public static UserProfileResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getBio(),
                user.getGithubUsername(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
