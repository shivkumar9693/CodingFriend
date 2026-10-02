package com.aicodingmentor.mentor.controller;

import com.aicodingmentor.mentor.dto.SocraBuddyDiagnosisRequest;
import com.aicodingmentor.mentor.dto.SocraBuddyHintRequest;
import com.aicodingmentor.mentor.model.HintLevel;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.problem.repository.ProblemRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import org.springframework.boot.test.mock.mockito.MockBean;
import com.aicodingmentor.mentor.service.MentorModelClient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("SocraBuddyMentorController Integration Tests")
class SocraBuddyMentorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private MentorModelClient mentorModelClient;

    private Problem testProblem;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByEmail("learner@test.com")) {
            User user = new User("Learner Test", "learner@test.com", "Password123!", UserRole.LEARNER);
            userRepository.save(user);
        }

        testProblem = new Problem(
                "Palindrome Number",
                "palindrome-number-" + UUID.randomUUID(),
                "Determine if an integer is a palindrome.",
                ProblemDifficulty.EASY,
                "-2^31 <= x <= 2^31 - 1",
                "x = 121",
                "true",
                "Example 1",
                "class Solution {}",
                "JAVA",
                Set.of("Math")
        );
        testProblem = problemRepository.save(testProblem);
    }

    @Test
    @DisplayName("Should reject unauthenticated hint requests with 401/403")
    void testRequestHint_unauthenticated_forbidden() throws Exception {
        SocraBuddyHintRequest request = new SocraBuddyHintRequest(
                testProblem.getId(),
                null,
                "class Solution {}",
                HintLevel.LEVEL_1_NUDGE,
                null,
                null
        );

        mockMvc.perform(post("/api/mentor/hints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "learner@test.com", roles = {"LEARNER"})
    @DisplayName("Should successfully return tiered Socratic hint when authenticated")
    void testRequestHint_authenticated_returnsHint() throws Exception {
        SocraBuddyHintRequest request = new SocraBuddyHintRequest(
                testProblem.getId(),
                null,
                "public class Solution { public boolean isPalindrome(int x) { return false; } }",
                HintLevel.LEVEL_1_NUDGE,
                "How do I handle negative numbers?",
                null
        );

        mockMvc.perform(post("/api/mentor/hints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hintLevel").value("LEVEL_1_NUDGE"))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.guidance").isNotEmpty())
                .andExpect(jsonPath("$.socraticQuestion").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "learner@test.com", roles = {"LEARNER"})
    @DisplayName("Should successfully return mistake diagnosis when authenticated")
    void testDiagnoseFailure_authenticated_returnsDiagnosis() throws Exception {
        SocraBuddyDiagnosisRequest request = new SocraBuddyDiagnosisRequest(
                testProblem.getId(),
                null,
                "public class Solution { public boolean isPalindrome(int x) { return x / 0 == 0; } }",
                null,
                "java.lang.ArithmeticException: / by zero",
                1
        );

        mockMvc.perform(post("/api/mentor/diagnose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mistakeClassification").isNotEmpty())
                .andExpect(jsonPath("$.diagnosisSummary").isNotEmpty())
                .andExpect(jsonPath("$.socraticGuidance").isNotEmpty());
    }

    @Test
    @DisplayName("Should reject unauthenticated inline-assist requests with 401/403")
    void testRequestInlineAssist_unauthenticated_forbidden() throws Exception {
        com.aicodingmentor.mentor.dto.InlineAssistRequest request = new com.aicodingmentor.mentor.dto.InlineAssistRequest(
                testProblem.getId(),
                "class Solution { public boolean isPalindrome(int x) { } }",
                1,
                "JAVA"
        );

        mockMvc.perform(post("/api/mentor/inline-assist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "learner@test.com", roles = {"LEARNER"})
    @DisplayName("Should successfully return inline Socratic micro-nudge when authenticated")
    void testRequestInlineAssist_authenticated_returnsInlineNudge() throws Exception {
        com.aicodingmentor.mentor.dto.InlineAssistRequest request = new com.aicodingmentor.mentor.dto.InlineAssistRequest(
                testProblem.getId(),
                "class Solution { public boolean isPalindrome(int x) { for (int i = 0; i < 10; i++) { for (int j = 0; j < 10; j++) {} } } }",
                1,
                "JAVA"
        );

        mockMvc.perform(post("/api/mentor/inline-assist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nudgeType").isNotEmpty())
                .andExpect(jsonPath("$.inlineMessage").isNotEmpty())
                .andExpect(jsonPath("$.targetLine").value(1))
                .andExpect(jsonPath("$.revealsSolution").value(false));
    }
}
