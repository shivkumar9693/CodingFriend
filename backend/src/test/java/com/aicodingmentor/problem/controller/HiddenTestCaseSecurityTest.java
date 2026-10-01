package com.aicodingmentor.problem.controller;

import com.aicodingmentor.auth.security.JwtTokenProvider;
import com.aicodingmentor.auth.security.UserPrincipal;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.problem.domain.TestCase;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.aicodingmentor.problem.repository.TestCaseRepository;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HiddenTestCaseSecurityTest {

    @Autowired
    private MockMvc mockMvc;

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
    private TestCase publicSampleTest;
    private TestCase hiddenSecretTest;
    private String learnerJwtToken;

    @BeforeEach
    void setUp() {
        problemId = UUID.randomUUID();
        problem = new Problem(
                "Secret Algorithm",
                "secret-algorithm",
                "Problem description",
                ProblemDifficulty.HARD,
                "Constraints",
                "Input",
                "Output",
                "Examples",
                "class Solution {}",
                "JAVA",
                Set.of("ALGORITHM")
        );
        problem.setId(problemId);

        publicSampleTest = new TestCase("public_input_1", "public_output_1", false, 1, "Public sample explanation");
        publicSampleTest.setId(UUID.randomUUID());

        hiddenSecretTest = new TestCase("hidden_super_secret_payload_12345", "hidden_expected_result_67890", true, 2, "Hidden internal note");
        hiddenSecretTest.setId(UUID.randomUUID());

        problem.addTestCase(publicSampleTest);
        problem.addTestCase(hiddenSecretTest);

        UUID learnerId = UUID.randomUUID();
        User learner = new User("learner@aicodingmentor.com", "hash", "Learner User", UserRole.USER);
        learner.setId(learnerId);

        when(userRepository.findById(learnerId)).thenReturn(Optional.of(learner));
        when(userRepository.findByEmail("learner@aicodingmentor.com")).thenReturn(Optional.of(learner));

        learnerJwtToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(learner));

        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));
        when(problemRepository.findBySlug("secret-algorithm")).thenReturn(Optional.of(problem));
        when(testCaseRepository.findByProblemIdAndIsHiddenFalseOrderBySequenceOrderAsc(problemId))
                .thenReturn(List.of(publicSampleTest));
    }

    @Test
    @DisplayName("SECURITY: Public unauthenticated GET /api/problems/{id} does not leak hidden test case data or secret payload")
    void testPublicProblemDetailDoesNotLeakHiddenTestCases() throws Exception {
        mockMvc.perform(get("/api/problems/" + problemId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sampleTestCases", hasSize(1)))
                .andExpect(jsonPath("$.sampleTestCases[0].inputData").value("public_input_1"))
                .andExpect(jsonPath("$.sampleTestCases[0].expectedOutput").value("public_output_1"))
                .andExpect(content().string(not(containsString("hidden_super_secret_payload_12345"))))
                .andExpect(content().string(not(containsString("hidden_expected_result_67890"))))
                .andExpect(jsonPath("$.sampleTestCases[0].isHidden").doesNotExist());
    }

    @Test
    @DisplayName("SECURITY: Authenticated USER role GET /api/problems/{id} does not leak hidden test case data")
    void testLearnerProblemDetailDoesNotLeakHiddenTestCases() throws Exception {
        mockMvc.perform(get("/api/problems/" + problemId)
                        .header("Authorization", "Bearer " + learnerJwtToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sampleTestCases", hasSize(1)))
                .andExpect(content().string(not(containsString("hidden_super_secret_payload_12345"))))
                .andExpect(content().string(not(containsString("hidden_expected_result_67890"))));
    }

    @Test
    @DisplayName("SECURITY: Public list endpoint GET /api/problems does not contain any test case payload")
    void testProblemCatalogDoesNotContainTestCases() throws Exception {
        when(problemRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(problem)));

        mockMvc.perform(get("/api/problems")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("hidden_super_secret_payload_12345"))))
                .andExpect(content().string(not(containsString("public_input_1"))));
    }

    @Test
    @DisplayName("SECURITY: Normal USER role is forbidden (403) from accessing admin test case endpoint")
    void testLearnerCannotAccessAdminTestCaseEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/problems/" + problemId + "/test-cases")
                        .header("Authorization", "Bearer " + learnerJwtToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("SECURITY: Unauthenticated request to admin test case endpoint is rejected with 401 Unauthorized")
    void testUnauthenticatedCannotAccessAdminTestCaseEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/problems/" + problemId + "/test-cases")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }
}
