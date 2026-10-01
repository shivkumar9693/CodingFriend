package com.aicodingmentor.problem.controller;

import com.aicodingmentor.problem.dto.CreateTestCaseRequest;
import com.aicodingmentor.problem.dto.TestCaseResponse;
import com.aicodingmentor.problem.dto.UpdateTestCaseRequest;
import com.aicodingmentor.problem.service.TestCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/problems/{problemId}/test-cases")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin Problem Test Cases", description = "Endpoints for administrators to manage public and hidden test cases")
public class AdminTestCaseController {

    private final TestCaseService testCaseService;

    public AdminTestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @GetMapping
    @Operation(summary = "Get all test cases for a problem (Admin only)",
            description = "Fetches all test cases including hidden grading suites. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test cases retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Problem not found")
    })
    public ResponseEntity<List<TestCaseResponse>> getTestCases(@PathVariable UUID problemId) {
        List<TestCaseResponse> testCases = testCaseService.getAdminTestCases(problemId);
        return ResponseEntity.ok(testCases);
    }

    @GetMapping("/{testCaseId}")
    @Operation(summary = "Get specific test case details (Admin only)",
            description = "Fetches single test case details including hidden flag and metadata. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test case retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TestCaseResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Test case or problem not found")
    })
    public ResponseEntity<TestCaseResponse> getTestCaseById(
            @PathVariable UUID problemId,
            @PathVariable UUID testCaseId
    ) {
        TestCaseResponse testCase = testCaseService.getAdminTestCaseById(problemId, testCaseId);
        return ResponseEntity.ok(testCase);
    }

    @PostMapping
    @Operation(summary = "Add a test case to a problem (Admin only)",
            description = "Creates a new test case for the specified problem. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Test case created successfully",
                    content = @Content(schema = @Schema(implementation = TestCaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Problem not found")
    })
    public ResponseEntity<TestCaseResponse> createTestCase(
            @PathVariable UUID problemId,
            @Valid @RequestBody CreateTestCaseRequest request
    ) {
        TestCaseResponse created = testCaseService.createTestCase(problemId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{testCaseId}")
    @Operation(summary = "Update an existing test case (Admin only)",
            description = "Updates test case data, hidden status, sequence order, or explanation. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test case updated successfully",
                    content = @Content(schema = @Schema(implementation = TestCaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Test case or problem not found")
    })
    public ResponseEntity<TestCaseResponse> updateTestCase(
            @PathVariable UUID problemId,
            @PathVariable UUID testCaseId,
            @Valid @RequestBody UpdateTestCaseRequest request
    ) {
        TestCaseResponse updated = testCaseService.updateTestCase(problemId, testCaseId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{testCaseId}")
    @Operation(summary = "Delete a test case from a problem (Admin only)",
            description = "Deletes a test case. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Test case deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Test case or problem not found")
    })
    public ResponseEntity<Void> deleteTestCase(
            @PathVariable UUID problemId,
            @PathVariable UUID testCaseId
    ) {
        testCaseService.deleteTestCase(problemId, testCaseId);
        return ResponseEntity.noContent().build();
    }
}
