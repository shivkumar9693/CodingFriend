package com.aicodingmentor.mentor.controller;

import com.aicodingmentor.auth.security.UserPrincipal;
import com.aicodingmentor.mentor.dto.SocraBuddyDiagnosisRequest;
import com.aicodingmentor.mentor.dto.SocraBuddyDiagnosisResponse;
import com.aicodingmentor.mentor.dto.SocraBuddyHintRequest;
import com.aicodingmentor.mentor.dto.SocraBuddyHintResponse;
import com.aicodingmentor.mentor.service.SocraBuddyMentorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/mentor")
@Tag(name = "SocraBuddy AI Mentor", description = "Socratic AI guidance, tiered hints, and mistake diagnosis")
public class SocraBuddyMentorController {

    private final SocraBuddyMentorService mentorService;

    public SocraBuddyMentorController(SocraBuddyMentorService mentorService) {
        this.mentorService = mentorService;
    }

    @PostMapping("/hints")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a tiered Socratic hint from SocraBuddy", description = "Returns non-spoiler Socratic guidance according to requested hint tier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hint generated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error in hint request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "Problem not found")
    })
    public ResponseEntity<SocraBuddyHintResponse> requestHint(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody SocraBuddyHintRequest request
    ) {
        UUID userId = currentUser != null ? currentUser.getId() : UUID.randomUUID();
        SocraBuddyHintResponse response = mentorService.generateHint(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/diagnose")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Diagnose submission failure with SocraBuddy", description = "Analyzes execution error and provides Socratic diagnostic insight")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Diagnosis generated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error in diagnosis request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "Problem not found")
    })
    public ResponseEntity<SocraBuddyDiagnosisResponse> diagnoseFailure(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody SocraBuddyDiagnosisRequest request
    ) {
        UUID userId = currentUser != null ? currentUser.getId() : UUID.randomUUID();
        SocraBuddyDiagnosisResponse response = mentorService.diagnoseMistake(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/inline-assist")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get real-time inline Socratic micro-nudge for Monaco Editor", description = "Analyzes active line and gives a non-spoiler 1-sentence thought prompt")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inline assist nudge generated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error in request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "Problem not found")
    })
    public ResponseEntity<com.aicodingmentor.mentor.dto.InlineAssistResponse> requestInlineAssist(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody com.aicodingmentor.mentor.dto.InlineAssistRequest request
    ) {
        UUID userId = currentUser != null ? currentUser.getId() : UUID.randomUUID();
        com.aicodingmentor.mentor.dto.InlineAssistResponse response = mentorService.generateInlineAssist(userId, request);
        return ResponseEntity.ok(response);
    }
}
