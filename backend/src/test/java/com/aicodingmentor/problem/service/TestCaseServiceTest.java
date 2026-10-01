package com.aicodingmentor.problem.service;

import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.problem.domain.TestCase;
import com.aicodingmentor.problem.dto.CreateTestCaseRequest;
import com.aicodingmentor.problem.dto.TestCaseResponse;
import com.aicodingmentor.problem.dto.UpdateTestCaseRequest;
import com.aicodingmentor.problem.exception.ProblemNotFoundException;
import com.aicodingmentor.problem.exception.TestCaseNotFoundException;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.aicodingmentor.problem.repository.TestCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestCaseServiceTest {

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    @InjectMocks
    private TestCaseServiceImpl testCaseService;

    private UUID problemId;
    private UUID testCaseId;
    private Problem problem;
    private TestCase testCase;

    @BeforeEach
    void setUp() {
        problemId = UUID.randomUUID();
        testCaseId = UUID.randomUUID();

        problem = new Problem(
                "Sample Problem", "sample-problem", "Desc", ProblemDifficulty.EASY,
                null, null, null, null, null, "JAVA", Set.of("ARRAY")
        );
        problem.setId(problemId);

        testCase = new TestCase("in_data", "out_data", true, 1, "Explanation", "{\"meta\":1}");
        testCase.setId(testCaseId);
        testCase.setProblem(problem);
    }

    @Test
    @DisplayName("getAdminTestCases returns all test cases for problem")
    void getAdminTestCases_Success() {
        when(problemRepository.existsById(problemId)).thenReturn(true);
        when(testCaseRepository.findByProblemIdOrderBySequenceOrderAsc(problemId))
                .thenReturn(List.of(testCase));

        List<TestCaseResponse> responses = testCaseService.getAdminTestCases(problemId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("in_data", responses.get(0).inputData());
        assertTrue(responses.get(0).isHidden());
    }

    @Test
    @DisplayName("getAdminTestCases throws ProblemNotFoundException if problem does not exist")
    void getAdminTestCases_ProblemNotFound() {
        when(problemRepository.existsById(problemId)).thenReturn(false);

        assertThrows(ProblemNotFoundException.class, () -> testCaseService.getAdminTestCases(problemId));
    }

    @Test
    @DisplayName("getAdminTestCaseById returns test case details")
    void getAdminTestCaseById_Success() {
        when(problemRepository.existsById(problemId)).thenReturn(true);
        when(testCaseRepository.findByIdAndProblemId(testCaseId, problemId)).thenReturn(Optional.of(testCase));

        TestCaseResponse response = testCaseService.getAdminTestCaseById(problemId, testCaseId);

        assertNotNull(response);
        assertEquals(testCaseId, response.id());
        assertEquals("out_data", response.expectedOutput());
    }

    @Test
    @DisplayName("getAdminTestCaseById throws TestCaseNotFoundException if test case not found")
    void getAdminTestCaseById_NotFound() {
        when(problemRepository.existsById(problemId)).thenReturn(true);
        when(testCaseRepository.findByIdAndProblemId(testCaseId, problemId)).thenReturn(Optional.empty());

        assertThrows(TestCaseNotFoundException.class, () -> testCaseService.getAdminTestCaseById(problemId, testCaseId));
    }

    @Test
    @DisplayName("createTestCase attaches new test case to problem and persists")
    void createTestCase_Success() {
        CreateTestCaseRequest request = new CreateTestCaseRequest("new_in", "new_out", false, 2, "exp", "{}");
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));
        when(testCaseRepository.save(any(TestCase.class))).thenAnswer(invocation -> {
            TestCase tc = invocation.getArgument(0);
            tc.setId(UUID.randomUUID());
            return tc;
        });

        TestCaseResponse response = testCaseService.createTestCase(problemId, request);

        assertNotNull(response);
        assertEquals("new_in", response.inputData());
        assertEquals("new_out", response.expectedOutput());
        assertFalse(response.isHidden());
        verify(testCaseRepository, times(1)).save(any(TestCase.class));
    }

    @Test
    @DisplayName("updateTestCase updates fields and persists")
    void updateTestCase_Success() {
        UpdateTestCaseRequest request = new UpdateTestCaseRequest("updated_in", "updated_out", true, 3, "updated_exp", "{}");
        when(problemRepository.existsById(problemId)).thenReturn(true);
        when(testCaseRepository.findByIdAndProblemId(testCaseId, problemId)).thenReturn(Optional.of(testCase));
        when(testCaseRepository.save(any(TestCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestCaseResponse response = testCaseService.updateTestCase(problemId, testCaseId, request);

        assertNotNull(response);
        assertEquals("updated_in", response.inputData());
        assertEquals("updated_out", response.expectedOutput());
        assertTrue(response.isHidden());
        assertEquals(3, response.sequenceOrder());
    }

    @Test
    @DisplayName("deleteTestCase removes test case from problem")
    void deleteTestCase_Success() {
        when(problemRepository.existsById(problemId)).thenReturn(true);
        when(testCaseRepository.findByIdAndProblemId(testCaseId, problemId)).thenReturn(Optional.of(testCase));

        testCaseService.deleteTestCase(problemId, testCaseId);

        verify(testCaseRepository, times(1)).delete(testCase);
    }

    @Test
    @DisplayName("getTestCasesForExecution returns raw entities for isolated execution engine")
    void getTestCasesForExecution_Success() {
        when(problemRepository.existsById(problemId)).thenReturn(true);
        when(testCaseRepository.findByProblemIdOrderBySequenceOrderAsc(problemId)).thenReturn(List.of(testCase));

        List<TestCase> testCases = testCaseService.getTestCasesForExecution(problemId);

        assertNotNull(testCases);
        assertEquals(1, testCases.size());
        assertEquals(testCaseId, testCases.get(0).getId());
    }
}
