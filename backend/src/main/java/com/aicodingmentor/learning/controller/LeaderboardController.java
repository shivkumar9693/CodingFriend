package com.aicodingmentor.learning.controller;

import com.aicodingmentor.learning.dto.LeaderboardEntryResponse;
import com.aicodingmentor.learning.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@Tag(name = "Leaderboard & Global Rankings", description = "Endpoints for student rankings, ELO ratings, and mastery tiers")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    @Operation(summary = "Get global learner leaderboard rankings")
    public ResponseEntity<List<LeaderboardEntryResponse>> getLeaderboard() {
        List<LeaderboardEntryResponse> entries = leaderboardService.getGlobalLeaderboard();
        return ResponseEntity.ok(entries);
    }
}
