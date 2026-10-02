package com.aicodingmentor.problem.service;

import com.aicodingmentor.problem.domain.TestCase;
import com.aicodingmentor.problem.dto.CreateTestCaseRequest;
import com.aicodingmentor.problem.dto.TestCaseResponse;
import com.aicodingmentor.problem.dto.UpdateTestCaseRequest;

import java.util.List;
import java.util.UUID;

public interface TestCaseService {

    List<TestCaseResponse> getAdminTestCases(UUID problemId);

    TestCaseResponse getAdminTestCaseById(UUID problemId, UUID testCaseId);

    TestCaseResponse createTestCase(UUID problemId, CreateTestCaseRequest request);

    TestCaseResponse updateTestCase(UUID problemId, UUID testCaseId, UpdateTestCaseRequest request);

    void deleteTestCase(UUID problemId, UUID testCaseId);

    List<TestCase> getTestCasesForExecution(UUID problemId);

    List<TestCase> getPublicTestCasesForExecution(UUID problemId);
}
