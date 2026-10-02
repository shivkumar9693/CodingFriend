package com.aicodingmentor.submission.controller;

import com.aicodingmentor.auth.security.JwtTokenProvider;
import com.aicodingmentor.auth.security.UserPrincipal;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.aicodingmentor.submission.domain.Submission;
import com.aicodingmentor.submission.domain.SubmissionStatus;
import com.aicodingmentor.submission.dto.CreateSubmissionRequest;
import com.aicodingmentor.submission.repository.SubmissionRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubmissionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private SubmissionRepository submissionRepository;

    @MockBean
    private ProblemRepository problemRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private com.aicodingmentor.mentor.service.MentorModelClient mentorModelClient;

    private User studentUser;
    private User otherStudentUser;
    private User adminUser;

    private String studentToken;
    private String otherStudentToken;
    private String adminToken;

    private UUID problemId;
    private UUID submissionId;
    private Submission submission;

    @BeforeEach
    void setUp() {
        studentUser = new User("student@example.com", "hash", "Student User", UserRole.USER);
        studentUser.setId(UUID.randomUUID());

        otherStudentUser = new User("other@example.com", "hash", "Other User", UserRole.USER);
        otherStudentUser.setId(UUID.randomUUID());

        adminUser = new User("admin@example.com", "hash", "Admin User", UserRole.ADMIN);
        adminUser.setId(UUID.randomUUID());

        studentToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(studentUser));
        otherStudentToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(otherStudentUser));
        adminToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(adminUser));

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(studentUser));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherStudentUser));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));

        problemId = UUID.randomUUID();
        submissionId = UUID.randomUUID();

        submission = new Submission(
                studentUser.getId(),
                problemId,
                "class Solution { public int[] twoSum() { return new int[]{}; } }",
                "JAVA"
        );
    }

    @Test
    @DisplayName("POST /api/submissions without token returns 401 Unauthorized")
    void createSubmission_Unauthenticated_Returns401() throws Exception {
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                problemId,
                "class Solution {}",
                "JAVA"
        );

        mockMvc.perform(post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/submissions with invalid payload returns 400 Bad Request")
    void createSubmission_InvalidPayload_Returns400() throws Exception {
        CreateSubmissionRequest invalidRequest = new CreateSubmissionRequest(
                null, // Missing problem ID
                "  ", // Blank source code
                "RUST" // Unsupported language
        );

        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    @DisplayName("POST /api/submissions for non-existent problem returns 404 Not Found")
    void createSubmission_ProblemNotFound_Returns404() throws Exception {
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                problemId,
                "class Solution {}",
                "JAVA"
        );

        when(problemRepository.findById(problemId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("POST /api/submissions with valid data creates submission in QUEUED state")
    void createSubmission_Success_Returns201() throws Exception {
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                problemId,
                "class Solution { public int[] twoSum() { return new int[]{}; } }",
                "JAVA"
        );

        Problem sampleProblem = new Problem("Two Sum", "two-sum", "Desc", ProblemDifficulty.EASY, "", "", "", "", "", "JAVA", Set.of());
        sampleProblem.setId(problemId);
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(sampleProblem));
        when(submissionRepository.save(any(Submission.class))).thenReturn(submission);

        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("QUEUED")))
                .andExpect(jsonPath("$.language", is("JAVA")))
                .andExpect(jsonPath("$.userId", is(studentUser.getId().toString())))
                .andExpect(jsonPath("$.problemId", is(problemId.toString())));
    }

    @Test
    @DisplayName("GET /api/submissions/{id} as owner returns 200 OK")
    void getSubmissionById_AsOwner_Returns200() throws Exception {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

        mockMvc.perform(get("/api/submissions/{id}", submissionId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(studentUser.getId().toString())))
                .andExpect(jsonPath("$.status", is("QUEUED")));
    }

    @Test
    @DisplayName("GET /api/submissions/{id} as non-owner returns 403 Forbidden")
    void getSubmissionById_AsOtherUser_Returns403() throws Exception {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

        mockMvc.perform(get("/api/submissions/{id}", submissionId)
                        .header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("GET /api/submissions/{id} as Admin returns 200 OK")
    void getSubmissionById_AsAdmin_Returns200() throws Exception {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

        mockMvc.perform(get("/api/submissions/{id}", submissionId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(studentUser.getId().toString())));
    }

    @Test
    @DisplayName("GET /api/submissions returns paginated list of submissions")
    void getSubmissions_Paginated_Returns200() throws Exception {
        when(submissionRepository.findByUserId(eq(studentUser.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(submission)));

        mockMvc.perform(get("/api/submissions")
                        .header("Authorization", "Bearer " + studentToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageNumber", is(0)))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    @DisplayName("POST /api/submissions/{id}/hints generates Level 1 Socratic hint")
    void requestSubmissionHint_Level1_Returns200() throws Exception {
        Problem problem = new Problem("Two Sum", "two-sum", "Desc", ProblemDifficulty.EASY, "", "", "", "", "", "JAVA", Set.of());
        problem.setId(submission.getProblemId());
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
        when(problemRepository.findById(submission.getProblemId())).thenReturn(Optional.of(problem));

        com.aicodingmentor.mentor.dto.SubmissionHintRequest request =
                new com.aicodingmentor.mentor.dto.SubmissionHintRequest(1);

        mockMvc.perform(post("/api/submissions/{id}/hints", submissionId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hintLevel", is(1)))
                .andExpect(jsonPath("$.revealsSolution", is(false)));
    }
}
