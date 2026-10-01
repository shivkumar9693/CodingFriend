package com.aicodingmentor.user.service;

import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.domain.UserRole;
import com.aicodingmentor.user.dto.UpdateProfileRequest;
import com.aicodingmentor.user.dto.UserProfileResponse;
import com.aicodingmentor.user.exception.UserNotFoundException;
import com.aicodingmentor.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User("dev@example.com", "encoded_password", "Original Name", UserRole.USER);
        user.setId(userId);
        user.setBio("Original bio");
        user.setGithubUsername("original-gh");
    }

    @Test
    @DisplayName("getProfile successfully returns UserProfileResponse when user exists")
    void getProfile_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getProfile(userId);

        assertNotNull(response);
        assertEquals(userId, response.id());
        assertEquals("dev@example.com", response.email());
        assertEquals("Original Name", response.fullName());
        assertEquals("Original bio", response.bio());
        assertEquals("original-gh", response.githubUsername());
        assertEquals(UserRole.USER, response.role());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getProfile throws UserNotFoundException when user is not found")
    void getProfile_NotFound_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getProfile(userId));
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getProfile throws UserNotFoundException when userId is null")
    void getProfile_NullUserId_ThrowsException() {
        assertThrows(UserNotFoundException.class, () -> userService.getProfile(null));
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("updateProfile successfully updates and persists profile fields")
    void updateProfile_Success() {
        UpdateProfileRequest request = new UpdateProfileRequest("Updated Name", "New coding bio", "new-github");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userService.updateProfile(userId, request);

        assertNotNull(response);
        assertEquals("Updated Name", response.fullName());
        assertEquals("New coding bio", response.bio());
        assertEquals("new-github", response.githubUsername());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("updateProfile trims input fields appropriately")
    void updateProfile_TrimsInputs() {
        UpdateProfileRequest request = new UpdateProfileRequest("  Trimmed Name  ", "  Trimmed Bio  ", "  trimmed_gh  ");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userService.updateProfile(userId, request);

        assertEquals("Trimmed Name", response.fullName());
        assertEquals("Trimmed Bio", response.bio());
        assertEquals("trimmed_gh", response.githubUsername());
    }

    @Test
    @DisplayName("updateProfile throws UserNotFoundException when user does not exist")
    void updateProfile_NotFound_ThrowsException() {
        UpdateProfileRequest request = new UpdateProfileRequest("Updated Name", "Bio", "gh");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.updateProfile(userId, request));
        verify(userRepository, never()).save(any());
    }
}
