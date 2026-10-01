package com.aicodingmentor.auth.controller;

import com.aicodingmentor.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@Tag(name = "Security Verification", description = "Endpoints for testing public, user, and admin role-based authorization")
public class TestSecurityController {

    @GetMapping("/public")
    @Operation(summary = "Public endpoint", description = "Accessible to unauthenticated visitors")
    public ResponseEntity<ApiResponse<Map<String, String>>> publicEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("Public content", Map.of("access", "public")));
    }

    @GetMapping("/user")
    @Operation(summary = "User protected endpoint", description = "Accessible to authenticated users with USER, LEARNER, or ADMIN roles")
    public ResponseEntity<ApiResponse<Map<String, String>>> userEndpoint(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                "Authenticated user content",
                Map.of("access", "user", "username", userDetails.getUsername())
        ));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin restricted endpoint", description = "Accessible strictly to users with ADMIN role")
    public ResponseEntity<ApiResponse<Map<String, String>>> adminEndpoint(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                "Admin content",
                Map.of("access", "admin", "username", userDetails.getUsername())
        ));
    }
}
