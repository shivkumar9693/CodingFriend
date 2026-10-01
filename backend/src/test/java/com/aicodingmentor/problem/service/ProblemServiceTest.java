package com.aicodingmentor.problem.service;

import com.aicodingmentor.common.api.PageResponse;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.problem.domain.TestCase;
import com.aicodingmentor.problem.dto.AdminProblemDetailResponse;
import com.aicodingmentor.problem.dto.CreateProblemRequest;
import com.aicodingmentor.problem.dto.CreateTestCaseRequest;
import com.aicodingmentor.problem.dto.ProblemDetailResponse;
import com.aicodingmentor.problem.dto.ProblemSummaryResponse;
import com.aicodingmentor.problem.dto.UpdateProblemRequest;
import com.aicodingmentor.problem.exception.DuplicateProblemSlugException;
import com.aicodingmentor.problem.exception.ProblemNotFoundException;
import com.aicodingmentor.execution.service.DriverHarnessRegistry;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.aicodingmentor.problem.repository.TestCaseRepository;
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
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProblemServiceTest {

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    @Mock
    private DriverHarnessRegistry driverHarnessRegistry;

    @InjectMocks
    private ProblemServiceImpl problemService;

    private UUID problemId;
    private Problem problem;
    private TestCase sampleTestCase;
    private TestCase hiddenTestCase;

    @BeforeEach
    void setUp() {
        problemId = UUID.randomUUID();
        problem = new Problem(
                "Two Sum",
                "two-sum",
                "Find two numbers that add up to target.",
                ProblemDifficulty.EASY,
                "2 <= nums.length <= 10^4",
                "nums array, target int",
                "int array of indices",
                "Input: nums = [2,7], target = 9 -> Output: [0,1]",
                "class Solution {}",
                "JAVA",
                Set.of("ARRAY", "HASH_TABLE")
        );
        problem.setId(problemId);

        sampleTestCase = new TestCase("[2, 7]\n9", "[0, 1]", false, 1, "Sample 1");
        sampleTestCase.setId(UUID.randomUUID());
        sampleTestCase.setProblem(problem);

        hiddenTestCase = new TestCase("[3, 3]\n6", "[0, 1]", true, 2, null);
        hiddenTestCase.setId(UUID.randomUUID());
        hiddenTestCase.setProblem(problem);

        problem.addTestCase(sampleTestCase);
        problem.addTestCase(hiddenTestCase);
    }

    @Test
    @DisplayName("getProblems returns paginated list of summary DTOs")
    void getProblems_ReturnsPaginatedSummaries() {
        Pageable pageable = PageRequest.of(0, 10);
        when(problemRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(problem), pageable, 1));

        PageResponse<ProblemSummaryResponse> response = problemService.getProblems(ProblemDifficulty.EASY, "ARRAY", pageable);

        assertNotNull(response);
        assertEquals(1, response.totalElements());
        assertEquals("Two Sum", response.content().get(0).title());
        assertEquals("two-sum", response.content().get(0).slug());
        assertEquals(ProblemDifficulty.EASY, response.content().get(0).difficulty());
    }

    @Test
    @DisplayName("getProblemByIdOrSlug by UUID returns problem with only sample test cases")
    void getProblemByIdOrSlug_ByUuid_MasksHiddenTestCases() {
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));
        when(testCaseRepository.findByProblemIdAndIsHiddenFalseOrderBySequenceOrderAsc(problemId))
                .thenReturn(List.of(sampleTestCase));

        ProblemDetailResponse response = problemService.getProblemByIdOrSlug(problemId.toString());

        assertNotNull(response);
        assertEquals("Two Sum", response.title());
        assertEquals(1, response.sampleTestCases().size());
        assertEquals("[2, 7]\n9", response.sampleTestCases().get(0).inputData());
        assertNull(response.sampleTestCases().get(0).isHidden(), "Hidden flag must be masked for public API");
    }

    @Test
    @DisplayName("getProblemByIdOrSlug by Slug returns problem details")
    void getProblemByIdOrSlug_BySlug_ReturnsDetails() {
        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(problem));
        when(testCaseRepository.findByProblemIdAndIsHiddenFalseOrderBySequenceOrderAsc(problemId))
                .thenReturn(List.of(sampleTestCase));

        ProblemDetailResponse response = problemService.getProblemByIdOrSlug("two-sum");

        assertNotNull(response);
        assertEquals("Two Sum", response.title());
    }

    @Test
    @DisplayName("getProblemByIdOrSlug throws ProblemNotFoundException when problem not found")
    void getProblemByIdOrSlug_NotFound_ThrowsException() {
        when(problemRepository.findBySlug("unknown-slug")).thenReturn(Optional.empty());

        assertThrows(ProblemNotFoundException.class, () -> problemService.getProblemByIdOrSlug("unknown-slug"));
    }

    @Test
    @DisplayName("getAdminProblemById returns problem with all test cases including hidden ones")
    void getAdminProblemById_ReturnsAllTestCases() {
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));
        when(testCaseRepository.findByProblemIdOrderBySequenceOrderAsc(problemId))
                .thenReturn(List.of(sampleTestCase, hiddenTestCase));

        AdminProblemDetailResponse response = problemService.getAdminProblemById(problemId);

        assertNotNull(response);
        assertEquals(2, response.testCases().size());
        assertTrue(response.testCases().get(1).isHidden());
    }

    @Test
    @DisplayName("createProblem successfully persists problem and generates slug")
    void createProblem_Success() {
        CreateTestCaseRequest tcReq = new CreateTestCaseRequest("[1, 2]", "[0, 1]", false, 1, "Sample");
        CreateProblemRequest request = new CreateProblemRequest(
                "Reverse Linked List",
                null,
                "Reverse a singly linked list.",
                ProblemDifficulty.EASY,
                "1 <= n <= 5000",
                "Head node",
                "Reversed head node",
                "Example 1",
                "class Solution {}",
                "JAVA",
                Set.of("LINKED_LIST"),
                List.of(tcReq)
        );

        when(problemRepository.existsBySlug("reverse-linked-list")).thenReturn(false);
        when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> {
            Problem p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        AdminProblemDetailResponse response = problemService.createProblem(request);

        assertNotNull(response);
        assertEquals("Reverse Linked List", response.title());
        assertEquals("reverse-linked-list", response.slug());
        assertEquals(1, response.testCases().size());
        verify(problemRepository, times(1)).save(any(Problem.class));
    }

    @Test
    @DisplayName("createProblem throws DuplicateProblemSlugException when slug exists")
    void createProblem_DuplicateSlug_ThrowsException() {
        CreateProblemRequest request = new CreateProblemRequest(
                "Two Sum",
                "two-sum",
                "Desc",
                ProblemDifficulty.EASY,
                null, null, null, null, null, "JAVA", null, null
        );

        when(problemRepository.existsBySlug("two-sum")).thenReturn(true);

        assertThrows(DuplicateProblemSlugException.class, () -> problemService.createProblem(request));
        verify(problemRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProblem successfully updates problem attributes and test cases")
    void updateProblem_Success() {
        UpdateProblemRequest request = new UpdateProblemRequest(
                "Two Sum Updated",
                "two-sum-updated",
                "Updated description",
                ProblemDifficulty.MEDIUM,
                "Updated constraints",
                "Input format",
                "Output format",
                "Examples",
                "Starter code",
                "JAVA",
                Set.of("ARRAY"),
                List.of(new CreateTestCaseRequest("[1]", "[1]", false, 1, "Sample"))
        );

        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));
        when(problemRepository.existsBySlugAndIdNot("two-sum-updated", problemId)).thenReturn(false);
        when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminProblemDetailResponse response = problemService.updateProblem(problemId, request);

        assertNotNull(response);
        assertEquals("Two Sum Updated", response.title());
        assertEquals("two-sum-updated", response.slug());
        assertEquals(ProblemDifficulty.MEDIUM, response.difficulty());
    }

    @Test
    @DisplayName("deleteProblem removes problem from database")
    void deleteProblem_Success() {
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));

        problemService.deleteProblem(problemId);

        verify(problemRepository, times(1)).delete(problem);
    }
}
