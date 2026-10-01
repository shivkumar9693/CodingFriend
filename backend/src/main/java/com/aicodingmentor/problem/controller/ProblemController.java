package com.aicodingmentor.problem.controller;

import com.aicodingmentor.common.api.PageResponse;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.problem.dto.AdminProblemDetailResponse;
import com.aicodingmentor.problem.dto.CreateProblemRequest;
import com.aicodingmentor.problem.dto.ProblemDetailResponse;
import com.aicodingmentor.problem.dto.ProblemSummaryResponse;
import com.aicodingmentor.problem.dto.UpdateProblemRequest;
import com.aicodingmentor.problem.service.ProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/problems")
@Tag(name = "Coding Problems", description = "Endpoints for exploring, viewing, and administering coding challenges")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @GetMapping
    @Operation(summary = "List coding problems with pagination & filtering",
            description = "Fetch paginated list of problem summaries, optionally filtered by difficulty and tag")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Problems retrieved successfully")
    })
    public ResponseEntity<PageResponse<ProblemSummaryResponse>> getProblems(
            @Parameter(description = "Filter by difficulty (EASY, MEDIUM, HARD)")
            @RequestParam(required = false) ProblemDifficulty difficulty,
            @Parameter(description = "Filter by tag (e.g. ARRAY, STRING, DYNAMIC_PROGRAMMING)")
            @RequestParam(required = false) String tag,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<ProblemSummaryResponse> response = problemService.getProblems(difficulty, tag, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get problem details for learners",
            description = "Fetch full problem statement, starter code, and public sample test cases (hidden test cases masked)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Problem details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ProblemDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Problem not found")
    })
    public ResponseEntity<ProblemDetailResponse> getProblemByIdOrSlug(
            @Parameter(description = "Problem UUID or unique URL slug")
            @PathVariable String id
    ) {
        ProblemDetailResponse response = problemService.getProblemByIdOrSlug(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create a new coding problem (Admin only)",
            description = "Creates a new problem along with its test suite. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Problem created successfully",
                    content = @Content(schema = @Schema(implementation = AdminProblemDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role"),
            @ApiResponse(responseCode = "409", description = "Conflict - Problem with slug already exists")
    })
    public ResponseEntity<AdminProblemDetailResponse> createProblem(
            @Valid @RequestBody CreateProblemRequest request
    ) {
        AdminProblemDetailResponse created = problemService.createProblem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update an existing coding problem (Admin only)",
            description = "Updates problem metadata and test cases. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Problem updated successfully",
                    content = @Content(schema = @Schema(implementation = AdminProblemDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Problem not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - Duplicate problem slug")
    })
    public ResponseEntity<AdminProblemDetailResponse> updateProblem(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProblemRequest request
    ) {
        AdminProblemDetailResponse updated = problemService.updateProblem(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Delete a coding problem (Admin only)",
            description = "Deletes a problem and all associated test cases. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Problem deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Problem not found")
    })
    public ResponseEntity<Void> deleteProblem(@PathVariable UUID id) {
        problemService.deleteProblem(id);
        return ResponseEntity.noContent().build();
    }
}
