package com.aicodingmentor.user.service;

import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.dto.UpdateProfileRequest;
import com.aicodingmentor.user.dto.UserProfileResponse;
import com.aicodingmentor.user.exception.UserNotFoundException;
import com.aicodingmentor.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final com.aicodingmentor.user.repository.UserDailyActivityRepository activityRepository;

    public UserServiceImpl(
            UserRepository userRepository,
            com.aicodingmentor.user.repository.UserDailyActivityRepository activityRepository
    ) {
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "user-profile", key = "#userId.toString()", unless = "#result == null")
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        if (userId == null) {
            throw new UserNotFoundException("User ID must not be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        return UserProfileResponse.from(user);
    }

    @Override
    @org.springframework.cache.annotation.Caching(evict = {
            @org.springframework.cache.annotation.CacheEvict(value = "user-profile", key = "#userId.toString()"),
            @org.springframework.cache.annotation.CacheEvict(value = "user-auth-id", key = "#userId.toString()"),
            @org.springframework.cache.annotation.CacheEvict(value = "user-auth-email", allEntries = true)
    })
    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        if (userId == null) {
            throw new UserNotFoundException("User ID must not be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        user.setFullName(request.fullName().trim());
        user.setBio(request.bio() != null ? request.bio().trim() : null);
        user.setGithubUsername(request.githubUsername() != null ? request.githubUsername().trim() : null);
        user.setPhoneNumber(request.phoneNumber() != null ? request.phoneNumber().trim() : null);
        user.setAvatarUrl(request.avatarUrl() != null ? request.avatarUrl().trim() : null);
        user.setLinkedinUrl(request.linkedinUrl() != null ? request.linkedinUrl().trim() : null);

        User savedUser = userRepository.save(user);
        log.info("Successfully updated profile for user id: {}", userId);

        return UserProfileResponse.from(savedUser);
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "user-activity", key = "#userId.toString()", unless = "#result == null")
    @Transactional(readOnly = true)
    public com.aicodingmentor.user.dto.ActivityHeatmapDto getActivityHeatmap(UUID userId) {
        if (userId == null) {
            throw new UserNotFoundException("User ID must not be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        java.time.LocalDate oneYearAgo = java.time.LocalDate.now().minusDays(365);
        java.util.List<com.aicodingmentor.user.domain.UserDailyActivity> records =
                activityRepository.findActivitiesSince(userId, oneYearAgo);

        int totalSubmissions = 0;
        int totalSolved = 0;
        java.util.List<com.aicodingmentor.user.dto.ActivityHeatmapDto.DayActivityDto> days = new java.util.ArrayList<>();

        for (com.aicodingmentor.user.domain.UserDailyActivity act : records) {
            totalSubmissions += act.getSubmissionCount();
            totalSolved += act.getProblemsSolved();
            days.add(new com.aicodingmentor.user.dto.ActivityHeatmapDto.DayActivityDto(
                    act.getActivityDate(),
                    act.getSubmissionCount(),
                    act.getProblemsSolved()
            ));
        }

        int currentStreak = user.getCurrentStreak() != null ? user.getCurrentStreak() : 0;
        int maxStreak = user.getMaxStreak() != null ? user.getMaxStreak() : 0;

        return new com.aicodingmentor.user.dto.ActivityHeatmapDto(
                currentStreak,
                maxStreak,
                days.size(),
                totalSubmissions,
                totalSolved,
                days
        );
    }

    @Override
    @Transactional
    public void recordSubmissionActivity(UUID userId, boolean isSolved) {
        if (userId == null) return;

        java.time.LocalDate today = java.time.LocalDate.now();
        com.aicodingmentor.user.domain.UserDailyActivity activity =
                activityRepository.findByUserIdAndActivityDate(userId, today)
                        .orElseGet(() -> new com.aicodingmentor.user.domain.UserDailyActivity(userId, today, 0, 0));

        activity.incrementSubmission();
        if (isSolved) {
            activity.incrementSolved();
        }
        activityRepository.save(activity);

        // Update user streak telemetry
        userRepository.findById(userId).ifPresent(user -> {
            java.time.LocalDate yesterday = today.minusDays(1);
            boolean hadActivityYesterday = activityRepository.findByUserIdAndActivityDate(userId, yesterday).isPresent();
            int currentStreak = user.getCurrentStreak() != null ? user.getCurrentStreak() : 0;

            if (activity.getSubmissionCount() == 1) { // First submission of the day
                if (hadActivityYesterday) {
                    currentStreak += 1;
                } else {
                    currentStreak = 1;
                }
            }

            user.setCurrentStreak(currentStreak);
            int maxStreak = user.getMaxStreak() != null ? user.getMaxStreak() : 0;
            if (currentStreak > maxStreak) {
                user.setMaxStreak(currentStreak);
            }
            userRepository.save(user);
        });
    }
}
