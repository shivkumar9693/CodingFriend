package com.aicodingmentor.problem.controller;

import com.aicodingmentor.auth.security.JwtTokenProvider;
import com.aicodingmentor.auth.security.UserPrincipal;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.problem.domain.TestCase;
import com.aicodingmentor.problem.dto.CreateTestCaseRequest;
import com.aicodingmentor.problem.dto.UpdateTestCaseRequest;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.aicodingmentor.problem.repository.TestCaseRepository;
import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.domain.UserRole;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminTestCaseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private ProblemRepository problemRepository;

    @MockBean
    private TestCaseRepository testCaseRepository;

    @MockBean
    private UserRepository userRepository;

    private UUID problemId;
    private UUID testCaseId;
    private Problem problem;
    private TestCase testCase;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        problemId = UUID.randomUUID();
        testCaseId = UUID.randomUUID();

        problem = new Problem(
                "Problem for Test Cases", "problem-test-cases", "Desc", ProblemDifficulty.EASY,
                null, null, null, null, null, "JAVA", Set.of("ARRAY")
        );
        problem.setId(problemId);

        testCase = new TestCase("[1, 2]", "[3]", true, 1, "Hidden grading suite", "{\"key\":\"val\"}");
        testCase.setId(testCaseId);
        testCase.setProblem(problem);

        UUID adminId = UUID.randomUUID();
        User admin = new User("admin@aicodingmentor.com", "hash", "Admin", UserRole.ADMIN);
        admin.setId(adminId);

        UUID userId = UUID.randomUUID();
        User user = new User("user@aicodingmentor.com", "hash", "User", UserRole.USER);
        user.setId(userId);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(userRepository.findByEmail("admin@aicodingmentor.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("user@aicodingmentor.com")).thenReturn(Optional.of(user));

        adminToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(admin));
        userToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(user));
    }

    @Test
    @DisplayName("GET /api/admin/problems/{problemId}/test-cases with ADMIN role returns 200 and all test cases")
    void getTestCases_Admin_Returns200() throws Exception {
        when(problemRepository.existsById(problemId)).thenReturn(true);
        when(testCaseRepository.findByProblemIdOrderBySequenceOrderAsc(problemId))
                .thenReturn(List.of(testCase));

        mockMvc.perform(get("/api/admin/problems/" + problemId + "/test-cases")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(testCaseId.toString()))
                .andExpect(jsonPath("$[0].isHidden").value(true));
    }

    @Test
    @DisplayName("GET /api/admin/problems/{problemId}/test-cases with USER role returns 403 Forbidden")
    void getTestCases_User_Returns403() throws Exception {
        mockMvc.perform(get("/api/admin/problems/" + problemId + "/test-cases")
                        .header("Authorization", "Bearer " + userToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/admin/problems/{problemId}/test-cases with ADMIN role returns 201 Created")
    void createTestCase_Admin_Returns201() throws Exception {
        CreateTestCaseRequest request = new CreateTestCaseRequest("[2, 3]", "[5]", true, 2, "Explanation", "{}");

        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));
        when(testCaseRepository.save(any(TestCase.class))).thenAnswer(invocation -> {
            TestCase tc = invocation.getArgument(0);
            tc.setId(UUID.randomUUID());
            return tc;
        });

        mockMvc.perform(post("/api/admin/problems/" + problemId + "/test-cases")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inputData").value("[2, 3]"))
                .andExpect(jsonPath("$.expectedOutput").value("[5]"))
                .andExpect(jsonPath("$.isHidden").value(true));
    }

    @Test
    @DisplayName("PUT /api/admin/problems/{problemId}/test-cases/{testCaseId} with ADMIN role returns 200 OK")
    void updateTestCase_Admin_Returns200() throws Exception {
        UpdateTestCaseRequest request = new UpdateTestCaseRequest("[2, 3, 4]", "[9]", false, 1, "Sample exp", "{}");

        when(problemRepository.existsById(problemId)).thenReturn(true);
        when(testCaseRepository.findByIdAndProblemId(testCaseId, problemId)).thenReturn(Optional.of(testCase));
        when(testCaseRepository.save(any(TestCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/admin/problems/" + problemId + "/test-cases/" + testCaseId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputData").value("[2, 3, 4]"))
                .andExpect(jsonPath("$.expectedOutput").value("[9]"))
                .andExpect(jsonPath("$.isHidden").value(false));
    }

    @Test
    @DisplayName("DELETE /api/admin/problems/{problemId}/test-cases/{testCaseId} with ADMIN role returns 204 No Content")
    void deleteTestCase_Admin_Returns204() throws Exception {
        when(problemRepository.existsById(problemId)).thenReturn(true);
        when(testCaseRepository.findByIdAndProblemId(testCaseId, problemId)).thenReturn(Optional.of(testCase));

        mockMvc.perform(delete("/api/admin/problems/" + problemId + "/test-cases/" + testCaseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }
}
