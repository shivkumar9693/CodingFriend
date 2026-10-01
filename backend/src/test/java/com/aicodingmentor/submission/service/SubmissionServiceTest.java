package com.aicodingmentor.submission.service;

import com.aicodingmentor.common.api.PageResponse;
import com.aicodingmentor.common.error.ResourceNotFoundException;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.aicodingmentor.submission.domain.Submission;
import com.aicodingmentor.submission.domain.SubmissionStatus;
import com.aicodingmentor.submission.dto.CreateSubmissionRequest;
import com.aicodingmentor.submission.dto.SubmissionResponse;
import com.aicodingmentor.submission.dto.SubmissionSummaryResponse;
import com.aicodingmentor.submission.exception.SubmissionAccessDeniedException;
import com.aicodingmentor.submission.exception.SubmissionNotFoundException;
import com.aicodingmentor.submission.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private SubmissionExecutionPlaceholderService executionPlaceholderService;

    @Mock
    private com.aicodingmentor.submission.kafka.SubmissionEventProducer eventProducer;

    @Mock
    private com.aicodingmentor.common.ratelimit.RateLimiterService rateLimiterService;

    private SubmissionServiceImpl submissionService;

    private UUID userId;
    private UUID problemId;
    private UUID submissionId;
    private Submission sampleSubmission;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        problemId = UUID.randomUUID();
        submissionId = UUID.randomUUID();

        sampleSubmission = new Submission(
                userId,
                problemId,
                "class Solution { public int[] twoSum(int[] nums, int target) { return new int[]{}; } }",
                "JAVA"
        );

        submissionService = new SubmissionServiceImpl(
                submissionRepository,
                problemRepository,
                executionPlaceholderService,
                eventProducer,
                rateLimiterService,
                false,
                30
        );
    }

    @Test
    @DisplayName("Successfully creates submission when problem exists")
    void createSubmission_Success() {
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                problemId,
                "class Solution { public int[] twoSum(int[] nums, int target) { return new int[]{}; } }",
                "JAVA"
        );

        when(rateLimiterService.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(true);
        com.aicodingmentor.problem.domain.Problem sampleProb = new com.aicodingmentor.problem.domain.Problem("Two Sum", "two-sum", "desc", com.aicodingmentor.problem.domain.ProblemDifficulty.EASY, "", "", "", "", "", "JAVA", java.util.Set.of());
        sampleProb.setId(problemId);
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(sampleProb));
        when(submissionRepository.save(any(Submission.class))).thenReturn(sampleSubmission);

        SubmissionResponse response = submissionService.createSubmission(userId, request);

        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertEquals(problemId, response.problemId());
        assertEquals("JAVA", response.language());
        assertEquals(SubmissionStatus.QUEUED, response.status());

        verify(problemRepository).findById(problemId);
        verify(submissionRepository).save(any(Submission.class));
        verify(executionPlaceholderService).enqueueForExecution(any());
    }

    @Test
    @DisplayName("Throws ApiException when submission rate limit is exceeded")
    void createSubmission_RateLimitExceeded_Throws() {
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                problemId,
                "class Solution {}",
                "JAVA"
        );

        when(rateLimiterService.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(false);

        assertThrows(com.aicodingmentor.common.error.ApiException.class, () ->
                submissionService.createSubmission(userId, request)
        );

        verify(submissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when problem does not exist")
    void createSubmission_ProblemNotFound_Throws() {
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                problemId,
                "class Solution {}",
                "JAVA"
        );

        when(rateLimiterService.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(problemRepository.findById(problemId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                submissionService.createSubmission(userId, request)
        );

        verify(submissionRepository, never()).save(any());
        verify(executionPlaceholderService, never()).enqueueForExecution(any());
    }

    @Test
    @DisplayName("Owner can retrieve their submission by ID")
    void getSubmissionById_AsOwner_Success() {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(sampleSubmission));

        SubmissionResponse response = submissionService.getSubmissionById(submissionId, userId, false);

        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertEquals(problemId, response.problemId());
    }

    @Test
    @DisplayName("Admin can retrieve any submission by ID")
    void getSubmissionById_AsAdmin_Success() {
        UUID otherUserId = UUID.randomUUID();
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(sampleSubmission));

        SubmissionResponse response = submissionService.getSubmissionById(submissionId, otherUserId, true);

        assertNotNull(response);
        assertEquals(userId, response.userId());
    }

    @Test
    @DisplayName("Non-owner learner is forbidden from viewing someone else's submission")
    void getSubmissionById_AsOtherUser_ThrowsAccessDenied() {
        UUID intruderId = UUID.randomUUID();
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(sampleSubmission));

        assertThrows(SubmissionAccessDeniedException.class, () ->
                submissionService.getSubmissionById(submissionId, intruderId, false)
        );
    }

    @Test
    @DisplayName("Throws SubmissionNotFoundException when submission ID does not exist")
    void getSubmissionById_NotFound_Throws() {
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.empty());

        assertThrows(SubmissionNotFoundException.class, () ->
                submissionService.getSubmissionById(submissionId, userId, false)
        );
    }

    @Test
    @DisplayName("Learner gets paginated list of only their submissions")
    void getSubmissions_AsLearner_FiltersByUserId() {
        Pageable pageable = PageRequest.of(0, 10);
        when(submissionRepository.findByUserId(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(sampleSubmission), pageable, 1));

        PageResponse<SubmissionSummaryResponse> response = submissionService.getSubmissions(
                userId,
                false,
                null,
                null,
                pageable
        );

        assertNotNull(response);
        assertEquals(1, response.totalElements());
        assertEquals(1, response.content().size());
        assertEquals(userId, response.content().get(0).userId());

        verify(submissionRepository).findByUserId(userId, pageable);
    }

    @Test
    @DisplayName("Learner gets paginated list filtered by problem ID")
    void getSubmissions_AsLearner_FiltersByProblemId() {
        Pageable pageable = PageRequest.of(0, 10);
        when(submissionRepository.findByUserIdAndProblemId(userId, problemId, pageable))
                .thenReturn(new PageImpl<>(List.of(sampleSubmission), pageable, 1));

        PageResponse<SubmissionSummaryResponse> response = submissionService.getSubmissions(
                userId,
                false,
                null,
                problemId,
                pageable
        );

        assertNotNull(response);
        assertEquals(1, response.totalElements());
        verify(submissionRepository).findByUserIdAndProblemId(userId, problemId, pageable);
    }

    @Test
    @DisplayName("Admin can view all submissions without user restriction")
    void getSubmissions_AsAdmin_FindsAll() {
        Pageable pageable = PageRequest.of(0, 10);
        when(submissionRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(sampleSubmission), pageable, 1));

        PageResponse<SubmissionSummaryResponse> response = submissionService.getSubmissions(
                userId,
                true,
                null,
                null,
                pageable
        );

        assertNotNull(response);
        assertEquals(1, response.totalElements());
        verify(submissionRepository).findAll(pageable);
    }
}
