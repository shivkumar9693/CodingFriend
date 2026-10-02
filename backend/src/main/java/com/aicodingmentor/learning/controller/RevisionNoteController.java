package com.aicodingmentor.learning.controller;

import com.aicodingmentor.auth.security.UserPrincipal;
import com.aicodingmentor.learning.domain.MasteryStatus;
import com.aicodingmentor.learning.dto.CreateRevisionNoteRequest;
import com.aicodingmentor.learning.dto.GeneratePostMortemRequest;
import com.aicodingmentor.learning.dto.PostMortemAnalysisResponse;
import com.aicodingmentor.learning.dto.RevisionNoteResponse;
import com.aicodingmentor.learning.dto.RevisionNotesSummaryDto;
import com.aicodingmentor.learning.dto.UpdateRevisionNoteRequest;
import com.aicodingmentor.learning.service.RevisionNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/learning/notes")
@Tag(name = "Revision Notes", description = "AI Post-Mortem counter-example generator and personal revision notebook")
@SecurityRequirement(name = "BearerAuth")
public class RevisionNoteController {

    private final RevisionNoteService revisionNoteService;

    public RevisionNoteController(RevisionNoteService revisionNoteService) {
        this.revisionNoteService = revisionNoteService;
    }

    @PostMapping("/analyze")
    @Operation(summary = "Generate AI Post-Mortem", description = "Analyzes a failed test case/submission and produces a structured counter-example discrepancy trace")
    public ResponseEntity<PostMortemAnalysisResponse> generatePostMortem(
            @Valid @RequestBody GeneratePostMortemRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(revisionNoteService.generatePostMortem(userId, request));
    }

    @PostMapping
    @Operation(summary = "Save Note to Revision Notebook", description = "Persists an AI Post-Mortem counter-example into the learner's personal revision notebook")
    public ResponseEntity<RevisionNoteResponse> createNote(
            @Valid @RequestBody CreateRevisionNoteRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(revisionNoteService.createNote(userId, request));
    }

    @GetMapping
    @Operation(summary = "List Saved Revision Notes", description = "Retrieves user's saved post-mortems with optional filtering by mastery status, bookmark, or search query")
    public ResponseEntity<List<RevisionNoteResponse>> getUserNotes(
            @RequestParam(required = false) MasteryStatus status,
            @RequestParam(required = false) Boolean bookmarked,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getId();
        return ResponseEntity.ok(revisionNoteService.getUserNotes(userId, status, bookmarked, search));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get Notebook Metrics", description = "Returns revision metrics: total saved notes, weak spots to revise, mastered count, and spaced repetition queue")
    public ResponseEntity<RevisionNotesSummaryDto> getSummary(@AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getId();
        return ResponseEntity.ok(revisionNoteService.getSummary(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Note by ID", description = "Retrieves details of a single revision note")
    public ResponseEntity<RevisionNoteResponse> getNoteById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getId();
        return ResponseEntity.ok(revisionNoteService.getNoteById(userId, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Note", description = "Updates reflections, bookmark, or mastery status for a note")
    public ResponseEntity<RevisionNoteResponse> updateNote(
            @PathVariable UUID id,
            @RequestBody UpdateRevisionNoteRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getId();
        return ResponseEntity.ok(revisionNoteService.updateNote(userId, id, request));
    }

    @PostMapping("/{id}/review")
    @Operation(summary = "Mark Note Reviewed", description = "Advances the spaced repetition interval for the specified note")
    public ResponseEntity<RevisionNoteResponse> markReviewed(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getId();
        return ResponseEntity.ok(revisionNoteService.markReviewed(userId, id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Note", description = "Deletes a revision note from learner's notebook")
    public ResponseEntity<Void> deleteNote(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal.getId();
        revisionNoteService.deleteNote(userId, id);
        return ResponseEntity.noContent().build();
    }
}
