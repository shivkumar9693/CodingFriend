package com.aicodingmentor.user.controller;

import com.aicodingmentor.auth.security.UserPrincipal;
import com.aicodingmentor.user.dto.UpdateProfileRequest;
import com.aicodingmentor.user.dto.UserProfileResponse;
import com.aicodingmentor.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Profile", description = "Endpoints for retrieving and updating user profile information")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;
    private final com.aicodingmentor.learning.service.LearnerMistakeTrackingService mistakeTrackingService;

    public UserController(
            UserService userService,
            com.aicodingmentor.learning.service.LearnerMistakeTrackingService mistakeTrackingService
    ) {
        this.userService = userService;
        this.mistakeTrackingService = mistakeTrackingService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile", description = "Retrieves profile details for the currently authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "User profile not found")
    })
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(@AuthenticationPrincipal UserPrincipal principal) {
        UserProfileResponse profile = userService.getProfile(principal.getId());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Updates profile attributes (name, bio, GitHub handle) for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error in request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "User profile not found")
    })
    public ResponseEntity<UserProfileResponse> updateCurrentUserProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserProfileResponse updatedProfile = userService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(updatedProfile);
    }

    @GetMapping("/me/mistakes")
    @Operation(summary = "Get current learner's recurring mistakes", description = "Retrieves mistake patterns, frequencies, and related topics for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mistakes retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token")
    })
    public ResponseEntity<java.util.List<com.aicodingmentor.learning.dto.LearnerMistakeSummaryResponse>> getLearnerMistakes(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        java.util.List<com.aicodingmentor.learning.dto.LearnerMistakeSummaryResponse> mistakes =
                mistakeTrackingService.getLearnerMistakes(principal.getId());
        return ResponseEntity.ok(mistakes);
    }

    @GetMapping("/me/statistics")
    @Operation(summary = "Get current learner's aggregate performance statistics", description = "Retrieves total submissions, success rate, and mistake breakdown")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token")
    })
    public ResponseEntity<com.aicodingmentor.learning.dto.LearnerStatisticsResponse> getLearnerStatistics(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        com.aicodingmentor.learning.dto.LearnerStatisticsResponse statistics =
                mistakeTrackingService.getLearnerStatistics(principal.getId());
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/me/heatmap")
    @Operation(summary = "Get current learner's 365-day activity heatmap and streak stats", description = "Retrieves daily submission volume and streaks for visualization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Heatmap retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token")
    })
    public ResponseEntity<com.aicodingmentor.user.dto.ActivityHeatmapDto> getLearnerHeatmap(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        com.aicodingmentor.user.dto.ActivityHeatmapDto heatmap = userService.getActivityHeatmap(principal.getId());
        return ResponseEntity.ok(heatmap);
    }
}
