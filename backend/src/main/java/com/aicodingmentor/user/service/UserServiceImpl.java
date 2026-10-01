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

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
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

        User savedUser = userRepository.save(user);
        log.info("Successfully updated profile for user id: {}", userId);

        return UserProfileResponse.from(savedUser);
    }
}
