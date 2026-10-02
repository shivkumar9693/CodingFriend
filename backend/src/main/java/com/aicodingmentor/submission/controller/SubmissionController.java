package com.aicodingmentor.submission.controller;

import com.aicodingmentor.auth.security.UserPrincipal;
import com.aicodingmentor.common.api.PageResponse;
import com.aicodingmentor.submission.dto.CreateSubmissionRequest;
import com.aicodingmentor.submission.dto.SubmissionResponse;
import com.aicodingmentor.submission.dto.SubmissionSummaryResponse;
import com.aicodingmentor.submission.service.SubmissionService;
import com.aicodingmentor.user.domain.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/submissions")
@Tag(name = "Submissions", description = "Endpoints for creating code submissions, polling execution state, and viewing submission history")
@SecurityRequirement(name = "BearerAuth")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final com.aicodingmentor.mentor.service.SubmissionHintService hintService;
    private final com.aicodingmentor.common.ratelimit.RateLimiterService rateLimiterService;

    public SubmissionController(
            SubmissionService submissionService,
            com.aicodingmentor.mentor.service.SubmissionHintService hintService,
            com.aicodingmentor.common.ratelimit.RateLimiterService rateLimiterService
    ) {
        this.submissionService = submissionService;
        this.hintService = hintService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping
    @Operation(summary = "Submit code for a problem", description = "Creates a new submission record in QUEUED state for isolated evaluation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Submission successfully created and queued",
                    content = @Content(schema = @Schema(implementation = SubmissionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error in submission request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "Target problem not found")
    })
    public ResponseEntity<SubmissionResponse> createSubmission(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateSubmissionRequest request
    ) {
        SubmissionResponse response = submissionService.createSubmission(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/run")
    @Operation(summary = "Execute code against public sample test cases without recording submission")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Code executed against public sample test cases",
                    content = @Content(schema = @Schema(implementation = SubmissionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error in execution payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "Target problem not found")
    })
    public ResponseEntity<SubmissionResponse> runCode(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateSubmissionRequest request
    ) {
        SubmissionResponse response = submissionService.runCode(principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get submission by ID", description = "Retrieves a single submission by its unique identifier. Only the submitter or an ADMIN can view it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Submission retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SubmissionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - You do not have permission to view this submission"),
            @ApiResponse(responseCode = "404", description = "Submission not found")
    })
    public ResponseEntity<SubmissionResponse> getSubmissionById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        boolean isAdmin = principal.getRole() == UserRole.ADMIN;
        SubmissionResponse response = submissionService.getSubmissionById(id, principal.getId(), isAdmin);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get paginated submissions", description = "Retrieves paginated submission history. Learners see their own submissions; Admins can see all.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Submissions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token")
    })
    public ResponseEntity<PageResponse<SubmissionSummaryResponse>> getSubmissions(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Filter submissions by problem ID")
            @RequestParam(required = false) UUID problemId,
            @Parameter(description = "Filter submissions by user ID (Admin only)")
            @RequestParam(required = false) UUID userId,
            @Parameter(description = "Zero-based page number")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1 to 50)")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field (createdAt, status)")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc, desc)")
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        int boundedPage = Math.max(0, page);
        int boundedSize = Math.min(Math.max(1, size), 50);

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String validSortBy = "status".equalsIgnoreCase(sortBy) ? "status" : "createdAt";
        Pageable pageable = PageRequest.of(boundedPage, boundedSize, Sort.by(direction, validSortBy));

        boolean isAdmin = principal.getRole() == UserRole.ADMIN;
        PageResponse<SubmissionSummaryResponse> response = submissionService.getSubmissions(
                principal.getId(),
                isAdmin,
                userId,
                problemId,
                pageable
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/hints")
    @Operation(summary = "Get tiered Socratic hint for a submission", description = "Generates non-spoiler tiered hints (Levels 1-5) for a specific submission attempt with anti-spoil progression guards")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hint generated successfully",
                    content = @Content(schema = @Schema(implementation = com.aicodingmentor.mentor.dto.SubmissionHintResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid level or progression constraint violation"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot access another user's submission"),
            @ApiResponse(responseCode = "404", description = "Submission not found")
    })
    public ResponseEntity<com.aicodingmentor.mentor.dto.SubmissionHintResponse> requestSubmissionHint(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody com.aicodingmentor.mentor.dto.SubmissionHintRequest request
    ) {
        if (!rateLimiterService.isAllowed("hint:" + principal.getId(), 60, 60)) {
            throw new com.aicodingmentor.common.error.ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Hint rate limit exceeded. Please wait a moment before requesting another hint."
            );
        }

        com.aicodingmentor.mentor.dto.SubmissionHintResponse response = hintService.generateSubmissionHint(
                principal.getId(),
                id,
                request
        );
        return ResponseEntity.ok(response);
    }
}
