package com.aicodingmentor.problem.service;

import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.TestCase;
import com.aicodingmentor.problem.dto.CreateTestCaseRequest;
import com.aicodingmentor.problem.dto.TestCaseResponse;
import com.aicodingmentor.problem.dto.UpdateTestCaseRequest;
import com.aicodingmentor.problem.exception.ProblemNotFoundException;
import com.aicodingmentor.problem.exception.TestCaseNotFoundException;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.aicodingmentor.problem.repository.TestCaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.aicodingmentor.common.config.RedisCacheConfig.CACHE_PROBLEMS;
import static com.aicodingmentor.common.config.RedisCacheConfig.CACHE_PROBLEM_DETAILS;

@Service
public class TestCaseServiceImpl implements TestCaseService {

    private static final Logger log = LoggerFactory.getLogger(TestCaseServiceImpl.class);

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;

    public TestCaseServiceImpl(ProblemRepository problemRepository, TestCaseRepository testCaseRepository) {
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestCaseResponse> getAdminTestCases(UUID problemId) {
        verifyProblemExists(problemId);

        List<TestCase> testCases = testCaseRepository.findByProblemIdOrderBySequenceOrderAsc(problemId);
        return testCases.stream()
                .map(tc -> TestCaseResponse.from(tc, true))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TestCaseResponse getAdminTestCaseById(UUID problemId, UUID testCaseId) {
        verifyProblemExists(problemId);

        TestCase testCase = testCaseRepository.findByIdAndProblemId(testCaseId, problemId)
                .orElseThrow(() -> new TestCaseNotFoundException("Test case not found with id: " + testCaseId + " for problem: " + problemId));

        return TestCaseResponse.from(testCase, true);
    }

    @Override
    @CacheEvict(value = {CACHE_PROBLEMS, CACHE_PROBLEM_DETAILS}, allEntries = true)
    @Transactional
    public TestCaseResponse createTestCase(UUID problemId, CreateTestCaseRequest request) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ProblemNotFoundException("Problem not found with id: " + problemId));

        TestCase testCase = new TestCase(
                request.inputData(),
                request.expectedOutput(),
                request.isHidden(),
                request.sequenceOrder(),
                request.explanation(),
                request.metadata()
        );

        problem.addTestCase(testCase);
        TestCase saved = testCaseRepository.save(testCase);
        log.info("Created test case ID: {} for problem ID: {} (hidden={})", saved.getId(), problemId, saved.isHidden());

        return TestCaseResponse.from(saved, true);
    }

    @Override
    @CacheEvict(value = {CACHE_PROBLEMS, CACHE_PROBLEM_DETAILS}, allEntries = true)
    @Transactional
    public TestCaseResponse updateTestCase(UUID problemId, UUID testCaseId, UpdateTestCaseRequest request) {
        verifyProblemExists(problemId);

        TestCase testCase = testCaseRepository.findByIdAndProblemId(testCaseId, problemId)
                .orElseThrow(() -> new TestCaseNotFoundException("Test case not found with id: " + testCaseId + " for problem: " + problemId));

        testCase.setInputData(request.inputData());
        testCase.setExpectedOutput(request.expectedOutput());
        testCase.setHidden(request.isHidden());
        testCase.setSequenceOrder(request.sequenceOrder());
        testCase.setExplanation(request.explanation());
        testCase.setMetadata(request.metadata());

        TestCase updated = testCaseRepository.save(testCase);
        log.info("Updated test case ID: {} for problem ID: {}", testCaseId, problemId);

        return TestCaseResponse.from(updated, true);
    }

    @Override
    @CacheEvict(value = {CACHE_PROBLEMS, CACHE_PROBLEM_DETAILS}, allEntries = true)
    @Transactional
    public void deleteTestCase(UUID problemId, UUID testCaseId) {
        verifyProblemExists(problemId);

        TestCase testCase = testCaseRepository.findByIdAndProblemId(testCaseId, problemId)
                .orElseThrow(() -> new TestCaseNotFoundException("Test case not found with id: " + testCaseId + " for problem: " + problemId));

        testCaseRepository.delete(testCase);
        log.info("Deleted test case ID: {} for problem ID: {}", testCaseId, problemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestCase> getTestCasesForExecution(UUID problemId) {
        verifyProblemExists(problemId);
        return testCaseRepository.findByProblemIdOrderBySequenceOrderAsc(problemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestCase> getPublicTestCasesForExecution(UUID problemId) {
        verifyProblemExists(problemId);
        return testCaseRepository.findByProblemIdAndIsHiddenFalseOrderBySequenceOrderAsc(problemId);
    }

    private void verifyProblemExists(UUID problemId) {
        if (problemId == null || !problemRepository.existsById(problemId)) {
            throw new ProblemNotFoundException("Problem not found with id: " + problemId);
        }
    }
}
