package com.aicodingmentor.common.controller;

import com.aicodingmentor.common.api.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @Value("${spring.application.name:ai-coding-mentor-backend}")
    private String applicationName;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> healthInfo = new LinkedHashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", applicationName);
        healthInfo.put("profile", activeProfile);
        healthInfo.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(ApiResponse.success("Service is healthy and operating normally", healthInfo));
    }
}
