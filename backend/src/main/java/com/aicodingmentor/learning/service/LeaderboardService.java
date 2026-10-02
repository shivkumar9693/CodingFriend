package com.aicodingmentor.learning.service;

import com.aicodingmentor.learning.dto.LeaderboardEntryResponse;

import java.util.List;

public interface LeaderboardService {
    List<LeaderboardEntryResponse> getGlobalLeaderboard();
}
