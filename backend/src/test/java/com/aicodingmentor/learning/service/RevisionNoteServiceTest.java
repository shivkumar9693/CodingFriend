package com.aicodingmentor.learning.service;

import com.aicodingmentor.learning.domain.MasteryStatus;
import com.aicodingmentor.learning.domain.MistakeCategory;
import com.aicodingmentor.learning.domain.RevisionNote;
import com.aicodingmentor.learning.dto.CreateRevisionNoteRequest;
import com.aicodingmentor.learning.dto.GeneratePostMortemRequest;
import com.aicodingmentor.learning.dto.PostMortemAnalysisResponse;
import com.aicodingmentor.learning.dto.RevisionNoteResponse;
import com.aicodingmentor.learning.dto.RevisionNotesSummaryDto;
import com.aicodingmentor.learning.dto.UpdateRevisionNoteRequest;
import com.aicodingmentor.learning.repository.RevisionNoteRepository;
import com.aicodingmentor.mentor.service.MentorModelClient;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RevisionNoteService Unit Tests")
class RevisionNoteServiceTest {

    @Mock
    private RevisionNoteRepository revisionNoteRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private MentorModelClient mentorModelClient;

    private ObjectMapper objectMapper;
    private RevisionNoteServiceImpl revisionNoteService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        objectMapper = new ObjectMapper();
        revisionNoteService = new RevisionNoteServiceImpl(
                revisionNoteRepository,
                problemRepository,
                mentorModelClient,
                objectMapper
        );
    }

    @Test
    @DisplayName("Should generate deterministic post-mortem when LLM client is offline")
    void testGenerateDeterministicPostMortemWhenLlmOffline() {
        when(mentorModelClient.isAvailable()).thenReturn(false);

        GeneratePostMortemRequest request = new GeneratePostMortemRequest(
                UUID.randomUUID(),
                "Two Sum",
                "JAVA",
                "class Solution { public int[] twoSum(int[] n, int t) { return new int[]{0,0}; } }",
                "WRONG_ANSWER",
                "nums = [3, 2, 4], target = 6",
                "[0, 0]",
                "[1, 2]",
                null
        );

        PostMortemAnalysisResponse response = revisionNoteService.generatePostMortem(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.mistakeCategory()).isEqualTo("LOGIC_ERROR");
        assertThat(response.studentFlowTrace()).contains("nums = [3, 2, 4], target = 6");
        assertThat(response.correctFlowTrace()).contains("[1, 2]");
        assertThat(response.keyTakeaway()).isNotEmpty();
    }

    @Test
    @DisplayName("Should generate accurate compile error post-mortem with line numbers and syntax diagnostics")
    void testGenerateCompileErrorPostMortem() {
        when(mentorModelClient.isAvailable()).thenReturn(false);

        GeneratePostMortemRequest request = new GeneratePostMortemRequest(
                UUID.randomUUID(),
                "Two Sum",
                "JAVA",
                "class Solution { public int[] twoSum(int[] nums, int target) { map.put(nums[i], i; } }",
                "COMPILE_ERROR",
                "Java 17 Compiler (Syntax Validation)",
                "MainDriver.java:14: error: ')' expected map.put(nums[i],i; ^ 1 error",
                "0 Compilation Errors (Clean Bytecode Build)",
                "/tmp/sandbox-exec-7621529037976171711/MainDriver.java:14: error: ')' expected map.put(nums[i],i; ^ 1 error"
        );

        PostMortemAnalysisResponse response = revisionNoteService.generatePostMortem(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.mistakeCategory()).isEqualTo("SYNTAX_ERROR");
        assertThat(response.conceptSummary()).contains("line 14");
        assertThat(response.conceptSummary()).contains("')' expected");
        assertThat(response.studentFlowTrace()).contains("line 14");
        assertThat(response.correctFlowTrace()).contains("balanced delimiters");
        assertThat(response.keyTakeaway()).contains("delimiters");
    }

    @Test
    @DisplayName("Should create and persist revision note successfully")
    void testCreateNote() {
        CreateRevisionNoteRequest request = new CreateRevisionNoteRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Two Sum",
                "two-sum",
                "JAVA",
                "OFF_BY_ONE",
                "Loop terminates early",
                "nums = [3, 2, 4]",
                "[0, 0]",
                "[1, 2]",
                "Trace of student code",
                "Trace of invariant logic",
                "Always check array.length - 1",
                "code here",
                "Remember for interview",
                true
        );

        when(revisionNoteRepository.save(any(RevisionNote.class))).thenAnswer(invocation -> {
            RevisionNote note = invocation.getArgument(0);
            return note;
        });

        RevisionNoteResponse response = revisionNoteService.createNote(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.problemTitle()).isEqualTo("Two Sum");
        assertThat(response.mistakeCategory()).isEqualTo(MistakeCategory.OFF_BY_ONE);
        assertThat(response.isBookmarked()).isTrue();
        assertThat(response.masteryStatus()).isEqualTo(MasteryStatus.TO_REVISE);

        verify(revisionNoteRepository, times(1)).save(any(RevisionNote.class));
    }

    @Test
    @DisplayName("Should update note reflections and mastery status")
    void testUpdateNote() {
        UUID noteId = UUID.randomUUID();
        RevisionNote existing = new RevisionNote(
                userId, UUID.randomUUID(), null, "Two Sum", "two-sum", "JAVA",
                MistakeCategory.LOGIC_ERROR, "Summary", "In", "Act", "Exp",
                "SFlow", "CFlow", "Takeaway", "Code", "Initial note"
        );

        when(revisionNoteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.of(existing));
        when(revisionNoteRepository.save(any(RevisionNote.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateRevisionNoteRequest updateReq = new UpdateRevisionNoteRequest(
                "Updated reflection after solving",
                MasteryStatus.MASTERED,
                true
        );

        RevisionNoteResponse updated = revisionNoteService.updateNote(userId, noteId, updateReq);

        assertThat(updated.userReflections()).isEqualTo("Updated reflection after solving");
        assertThat(updated.masteryStatus()).isEqualTo(MasteryStatus.MASTERED);
        assertThat(updated.isBookmarked()).isTrue();
    }

    @Test
    @DisplayName("Should advance spaced repetition review count")
    void testMarkReviewed() {
        UUID noteId = UUID.randomUUID();
        RevisionNote existing = new RevisionNote(
                userId, UUID.randomUUID(), null, "Two Sum", "two-sum", "JAVA",
                MistakeCategory.LOGIC_ERROR, "Summary", "In", "Act", "Exp",
                "SFlow", "CFlow", "Takeaway", "Code", "Reflection"
        );

        when(revisionNoteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.of(existing));
        when(revisionNoteRepository.save(any(RevisionNote.class))).thenAnswer(inv -> inv.getArgument(0));

        RevisionNoteResponse reviewed = revisionNoteService.markReviewed(userId, noteId);

        assertThat(reviewed.reviewCount()).isEqualTo(1);
        assertThat(reviewed.lastReviewedAt()).isNotNull();
        assertThat(reviewed.nextReviewAt()).isNotNull();
    }

    @Test
    @DisplayName("Should compute notebook summary statistics accurately")
    void testGetSummary() {
        when(revisionNoteRepository.countByUserId(userId)).thenReturn(5L);
        when(revisionNoteRepository.countByUserIdAndMasteryStatus(userId, MasteryStatus.TO_REVISE)).thenReturn(3L);
        when(revisionNoteRepository.countByUserIdAndMasteryStatus(userId, MasteryStatus.REVISED)).thenReturn(1L);
        when(revisionNoteRepository.countByUserIdAndMasteryStatus(userId, MasteryStatus.MASTERED)).thenReturn(1L);
        when(revisionNoteRepository.countByUserIdAndIsBookmarkedTrue(userId)).thenReturn(2L);
        when(revisionNoteRepository.countDueForReview(eq(userId), any())).thenReturn(2L);
        when(revisionNoteRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        RevisionNotesSummaryDto summary = revisionNoteService.getSummary(userId);

        assertThat(summary.totalNotes()).isEqualTo(5L);
        assertThat(summary.toReviseCount()).isEqualTo(3L);
        assertThat(summary.masteredCount()).isEqualTo(1L);
        assertThat(summary.bookmarkedCount()).isEqualTo(2L);
        assertThat(summary.dueTodayCount()).isEqualTo(2L);
    }
}
