package com.aicodingmentor.user.service;

import com.aicodingmentor.user.dto.UpdateProfileRequest;
import com.aicodingmentor.user.dto.UserProfileResponse;

import java.util.UUID;

public interface UserService {

    UserProfileResponse getProfile(UUID userId);

    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    com.aicodingmentor.user.dto.ActivityHeatmapDto getActivityHeatmap(UUID userId);

    void recordSubmissionActivity(UUID userId, boolean isSolved);
}
