package com.aicodingmentor.learning.dto;

import java.util.UUID;

public record LeaderboardEntryResponse(
        int rank,
        UUID userId,
        String fullName,
        String email,
        String avatarUrl,
        String tier,
        int rating,
        int solvedCount,
        int solvedEasy,
        int solvedMedium,
        int solvedHard,
        double accuracy,
        int streakDays,
        String badge
) {
}
