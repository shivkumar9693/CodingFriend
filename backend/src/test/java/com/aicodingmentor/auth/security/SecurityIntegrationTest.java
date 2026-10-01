package com.aicodingmentor.auth.security;

import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.domain.UserRole;
import com.aicodingmentor.user.repository.UserRepository;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserRepository userRepository;

    private User user;
    private User admin;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        user = new User("learner@aicodingmentor.com", "hash123", "Learner User", UserRole.USER);
        user.setId(userId);

        UUID adminId = UUID.randomUUID();
        admin = new User("admin@aicodingmentor.com", "hash456", "Admin User", UserRole.ADMIN);
        admin.setId(adminId);

        when(userRepository.findByEmail("learner@aicodingmentor.com")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("admin@aicodingmentor.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        userToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(user));
        adminToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(admin));
    }

    @Test
    @DisplayName("Public endpoints are accessible without authentication")
    void testPublicEndpointAccessible() throws Exception {
        mockMvc.perform(get("/api/test/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.access").value("public"));
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint returns 401 Unauthorized")
    void testProtectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/test/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Authenticated user with USER role can access user protected endpoint")
    void testUserCanAccessUserEndpoint() throws Exception {
        mockMvc.perform(get("/api/test/user")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("learner@aicodingmentor.com"));
    }

    @Test
    @DisplayName("User with USER role is denied access (403 Forbidden) to admin endpoint")
    void testUserDeniedAccessToAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/test/admin")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Access denied")));
    }

    @Test
    @DisplayName("Authenticated admin with ADMIN role can access admin protected endpoint")
    void testAdminCanAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/test/admin")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("admin@aicodingmentor.com"));
    }

    @Test
    @DisplayName("Request with invalid or tampered JWT returns 401 Unauthorized")
    void testInvalidTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/test/user")
                        .header("Authorization", "Bearer invalid.token.signature"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }
}
