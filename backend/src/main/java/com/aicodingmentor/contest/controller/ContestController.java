package com.aicodingmentor.contest.controller;

import com.aicodingmentor.auth.security.UserPrincipal;
import com.aicodingmentor.contest.dto.ContestDto;
import com.aicodingmentor.contest.dto.ContestLeaderboardDto;
import com.aicodingmentor.contest.dto.ContestParticipantDto;
import com.aicodingmentor.contest.dto.CreateContestRequest;
import com.aicodingmentor.contest.service.ContestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contests")
@Tag(name = "Contests", description = "Endpoints for competitive programming contests, live standings, and proctoring")
public class ContestController {

    private final ContestService contestService;

    public ContestController(ContestService contestService) {
        this.contestService = contestService;
    }

    @GetMapping
    @Operation(summary = "List all contests", description = "Retrieves upcoming, active, and past competitive programming contests")
    public ResponseEntity<List<ContestDto>> getAllContests(@AuthenticationPrincipal UserPrincipal principal) {
        UUID currentUserId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(contestService.getAllContests(currentUserId));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get contest by slug", description = "Retrieves contest details, problem schedule, and registration status")
    public ResponseEntity<ContestDto> getContestBySlug(
            @PathVariable String slug,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID currentUserId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(contestService.getContestBySlug(slug, currentUserId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create contest (Admin only)", description = "Admin creates a new scheduled contest with curated problems and points")
    public ResponseEntity<ContestDto> createContest(
            @Valid @RequestBody CreateContestRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID adminId = principal != null ? principal.getId() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(contestService.createContest(request, adminId));
    }

    @PostMapping("/{slug}/register")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Register for contest", description = "Registers the authenticated user for the designated contest")
    public ResponseEntity<ContestParticipantDto> register(
            @PathVariable String slug,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID currentUserId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(contestService.registerForContest(slug, currentUserId));
    }

    @GetMapping("/{slug}/leaderboard")
    @Operation(summary = "Get contest leaderboard", description = "Retrieves live or final ICPC standings for the contest")
    public ResponseEntity<ContestLeaderboardDto> getLeaderboard(@PathVariable String slug) {
        return ResponseEntity.ok(contestService.getLeaderboard(slug));
    }

    @PostMapping("/{slug}/tab-switch")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Report tab switch warning", description = "Records a client tab switch / blur event for contest anti-cheat audit")
    public ResponseEntity<Void> recordTabSwitch(
            @PathVariable String slug,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal != null) {
            contestService.recordTabSwitch(slug, principal.getId());
        }
        return ResponseEntity.ok().build();
    }
}
