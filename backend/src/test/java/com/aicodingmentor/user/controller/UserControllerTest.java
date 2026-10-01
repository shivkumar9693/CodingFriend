package com.aicodingmentor.user.controller;

import com.aicodingmentor.auth.security.JwtTokenProvider;
import com.aicodingmentor.auth.security.UserPrincipal;
import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.domain.UserRole;
import com.aicodingmentor.user.dto.UpdateProfileRequest;
import com.aicodingmentor.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserRepository userRepository;

    private UUID userId;
    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User("developer@aicodingmentor.com", "hashed_password", "Original Dev", UserRole.USER);
        testUser.setId(userId);
        testUser.setBio("Initial bio text");
        testUser.setGithubUsername("initial-dev");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("developer@aicodingmentor.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        jwtToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(testUser));
    }

    @Test
    @DisplayName("GET /api/users/me returns 200 and profile details for authenticated user")
    void getProfile_Authenticated_ReturnsProfile() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + jwtToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("developer@aicodingmentor.com"))
                .andExpect(jsonPath("$.fullName").value("Original Dev"))
                .andExpect(jsonPath("$.bio").value("Initial bio text"))
                .andExpect(jsonPath("$.githubUsername").value("initial-dev"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("GET /api/users/me without Authorization header returns 401 Unauthorized")
    void getProfile_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("PUT /api/users/me with valid payload returns 200 and updated profile")
    void updateProfile_ValidPayload_ReturnsUpdatedProfile() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Updated Name", "Updated developer bio", "updated-gh");

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.bio").value("Updated developer bio"))
                .andExpect(jsonPath("$.githubUsername").value("updated-gh"));
    }

    @Test
    @DisplayName("PUT /api/users/me without Authorization header returns 401 Unauthorized")
    void updateProfile_Unauthenticated_Returns401() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Updated Name", "Bio", "updated-gh");

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("PUT /api/users/me with invalid payload returns 400 Bad Request with validation errors")
    void updateProfile_InvalidPayload_Returns400() throws Exception {
        UpdateProfileRequest invalidRequest = new UpdateProfileRequest("", "Bio", "invalid@handle!");

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    @DisplayName("PUT /api/users/me with too short full name returns 400 Bad Request")
    void updateProfile_TooShortName_Returns400() throws Exception {
        UpdateProfileRequest invalidRequest = new UpdateProfileRequest("A", "Bio", "valid-handle");

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/users/me/mistakes returns 200 OK with list of mistakes")
    void getLearnerMistakes_Authenticated_Returns200() throws Exception {
        mockMvc.perform(get("/api/users/me/mistakes")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/users/me/statistics returns 200 OK with aggregate stats")
    void getLearnerStatistics_Authenticated_Returns200() throws Exception {
        mockMvc.perform(get("/api/users/me/statistics")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }
}
