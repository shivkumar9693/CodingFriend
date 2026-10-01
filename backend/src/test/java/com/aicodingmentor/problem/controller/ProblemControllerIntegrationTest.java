package com.aicodingmentor.problem.controller;

import com.aicodingmentor.auth.security.JwtTokenProvider;
import com.aicodingmentor.auth.security.UserPrincipal;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.problem.domain.TestCase;
import com.aicodingmentor.problem.dto.CreateProblemRequest;
import com.aicodingmentor.problem.dto.CreateTestCaseRequest;
import com.aicodingmentor.problem.dto.UpdateProblemRequest;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProblemControllerIntegrationTest {

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
    private Problem problem;
    private TestCase sampleTestCase;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        problemId = UUID.randomUUID();
        problem = new Problem(
                "Two Sum",
                "two-sum",
                "Find two numbers that sum to target",
                ProblemDifficulty.EASY,
                "2 <= nums.length <= 10^4",
                "nums array, target",
                "[i, j]",
                "Example 1",
                "class Solution {}",
                "JAVA",
                Set.of("ARRAY", "HASH_TABLE")
        );
        problem.setId(problemId);

        sampleTestCase = new TestCase("[2, 7]\n9", "[0, 1]", false, 1, "Sample explanation");
        sampleTestCase.setId(UUID.randomUUID());

        UUID userId = UUID.randomUUID();
        User normalUser = new User("learner@aicodingmentor.com", "hash", "Learner", UserRole.USER);
        normalUser.setId(userId);

        UUID adminId = UUID.randomUUID();
        User adminUser = new User("admin@aicodingmentor.com", "hash", "Admin", UserRole.ADMIN);
        adminUser.setId(adminId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(normalUser));
        when(userRepository.findByEmail("learner@aicodingmentor.com")).thenReturn(Optional.of(normalUser));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(userRepository.findByEmail("admin@aicodingmentor.com")).thenReturn(Optional.of(adminUser));

        userToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(normalUser));
        adminToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(adminUser));
    }

    @Test
    @DisplayName("GET /api/problems is publicly accessible and returns paginated problem summaries")
    void getProblems_PublicAccess_Returns200() throws Exception {
        when(problemRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(problem)));

        mockMvc.perform(get("/api/problems")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Two Sum"))
                .andExpect(jsonPath("$.content[0].slug").value("two-sum"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/problems/{id} is publicly accessible and masks hidden test cases")
    void getProblemById_PublicAccess_MasksHiddenTestCases() throws Exception {
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));
        when(testCaseRepository.findByProblemIdAndIsHiddenFalseOrderBySequenceOrderAsc(problemId))
                .thenReturn(List.of(sampleTestCase));

        mockMvc.perform(get("/api/problems/" + problemId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(problemId.toString()))
                .andExpect(jsonPath("$.title").value("Two Sum"))
                .andExpect(jsonPath("$.sampleTestCases").isArray())
                .andExpect(jsonPath("$.sampleTestCases", hasSize(1)))
                .andExpect(jsonPath("$.sampleTestCases[0].inputData").value("[2, 7]\n9"))
                .andExpect(jsonPath("$.sampleTestCases[0].isHidden").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/problems without authentication returns 401 Unauthorized")
    void createProblem_Unauthenticated_Returns401() throws Exception {
        CreateProblemRequest request = new CreateProblemRequest(
                "New Problem", "new-problem", "Desc", ProblemDifficulty.EASY,
                null, null, null, null, null, "JAVA", null, null
        );

        mockMvc.perform(post("/api/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/problems with USER role returns 403 Forbidden")
    void createProblem_UserRole_Returns403() throws Exception {
        CreateProblemRequest request = new CreateProblemRequest(
                "New Problem", "new-problem", "Desc", ProblemDifficulty.EASY,
                null, null, null, null, null, "JAVA", null, null
        );

        mockMvc.perform(post("/api/problems")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/problems with ADMIN role returns 201 Created")
    void createProblem_AdminRole_Returns201() throws Exception {
        CreateTestCaseRequest tc = new CreateTestCaseRequest("[1, 2]", "[0, 1]", false, 1, "Explanation");
        CreateProblemRequest request = new CreateProblemRequest(
                "New Admin Problem", "new-admin-problem", "Full description", ProblemDifficulty.MEDIUM,
                "Constraints", "Input", "Output", "Examples", "class Solution {}", "JAVA", Set.of("ARRAY"), List.of(tc)
        );

        when(problemRepository.existsBySlug("new-admin-problem")).thenReturn(false);
        when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> {
            Problem p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        mockMvc.perform(post("/api/problems")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Admin Problem"))
                .andExpect(jsonPath("$.slug").value("new-admin-problem"))
                .andExpect(jsonPath("$.difficulty").value("MEDIUM"));
    }

    @Test
    @DisplayName("PUT /api/problems/{id} with USER role returns 403 Forbidden")
    void updateProblem_UserRole_Returns403() throws Exception {
        UpdateProblemRequest request = new UpdateProblemRequest(
                "Updated", "updated", "Desc", ProblemDifficulty.EASY,
                null, null, null, null, null, "JAVA", null, null
        );

        mockMvc.perform(put("/api/problems/" + problemId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/problems/{id} with ADMIN role returns 200 OK")
    void updateProblem_AdminRole_Returns200() throws Exception {
        UpdateProblemRequest request = new UpdateProblemRequest(
                "Updated Two Sum", "two-sum", "Updated desc", ProblemDifficulty.EASY,
                "Constraints", "Input", "Output", "Examples", "Starter", "JAVA", Set.of("ARRAY"), null
        );

        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));
        when(problemRepository.existsBySlugAndIdNot("two-sum", problemId)).thenReturn(false);
        when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/problems/" + problemId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Two Sum"));
    }

    @Test
    @DisplayName("DELETE /api/problems/{id} with USER role returns 403 Forbidden")
    void deleteProblem_UserRole_Returns403() throws Exception {
        mockMvc.perform(delete("/api/problems/" + problemId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/problems/{id} with ADMIN role returns 204 No Content")
    void deleteProblem_AdminRole_Returns204() throws Exception {
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));

        mockMvc.perform(delete("/api/problems/" + problemId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }
}
