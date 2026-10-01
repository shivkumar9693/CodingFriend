package com.aicodingmentor.submission.service;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.mentor.repository.SubmissionHintRepository;
import com.aicodingmentor.submission.domain.Submission;
import com.aicodingmentor.submission.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionRetentionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private SubmissionHintRepository submissionHintRepository;

    @Captor
    private ArgumentCaptor<Collection<UUID>> deletedIdsCaptor;

    private SubmissionRetentionServiceImpl retentionService;

    private final UUID userId = UUID.randomUUID();
    private final UUID problemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        retentionService = new SubmissionRetentionServiceImpl(
                submissionRepository,
                submissionHintRepository,
                7,
                3
        );
    }

    private Submission createMockSubmission(UUID id, UUID uId, UUID pId, EvaluationVerdict verdict, long execTimeMs, Instant createdAt) {
        Submission sub = new Submission(uId, pId, "class Solution {}", "JAVA");
        ReflectionTestUtils.setField(sub, "id", id);
        ReflectionTestUtils.setField(sub, "verdict", verdict);
        ReflectionTestUtils.setField(sub, "executionTimeMs", execTimeMs);
        ReflectionTestUtils.setField(sub, "createdAt", createdAt);
        return sub;
    }

    @Test
    @DisplayName("Returns 0 when there are no stale submissions before cutoff")
    void purgeStaleSubmissions_whenNoOldSubmissions_returnsZero() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        when(submissionRepository.findByCreatedAtBefore(cutoff)).thenReturn(Collections.emptyList());

        int purged = retentionService.purgeStaleSubmissions(cutoff, 3);

        assertThat(purged).isEqualTo(0);
        verify(submissionRepository, never()).deleteAllByIdIn(any());
        verify(submissionHintRepository, never()).deleteBySubmissionIdIn(any());
    }

    @Test
    @DisplayName("Preserves top 3 accepted submissions and purges older non-accepted and surplus accepted submissions")
    void purgeStaleSubmissions_preservesTop3AcceptedAndDeletesStaleOthers() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant eightDaysAgo = Instant.now().minus(8, ChronoUnit.DAYS);
        Instant nineDaysAgo = Instant.now().minus(9, ChronoUnit.DAYS);
        Instant tenDaysAgo = Instant.now().minus(10, ChronoUnit.DAYS);
        Instant elevenDaysAgo = Instant.now().minus(11, ChronoUnit.DAYS);
        Instant twelveDaysAgo = Instant.now().minus(12, ChronoUnit.DAYS);

        // Top 3 accepted submissions for user & problem (fastest execution times)
        UUID acc1Id = UUID.randomUUID(); // Fastest (50ms)
        UUID acc2Id = UUID.randomUUID(); // 60ms
        UUID acc3Id = UUID.randomUUID(); // 70ms
        UUID acc4Id = UUID.randomUUID(); // 4th accepted (120ms - surplus)
        UUID fail1Id = UUID.randomUUID(); // Wrong answer
        UUID fail2Id = UUID.randomUUID(); // Compile error

        Submission acc1 = createMockSubmission(acc1Id, userId, problemId, EvaluationVerdict.ACCEPTED, 50L, eightDaysAgo);
        Submission acc2 = createMockSubmission(acc2Id, userId, problemId, EvaluationVerdict.ACCEPTED, 60L, nineDaysAgo);
        Submission acc3 = createMockSubmission(acc3Id, userId, problemId, EvaluationVerdict.ACCEPTED, 70L, tenDaysAgo);
        Submission acc4 = createMockSubmission(acc4Id, userId, problemId, EvaluationVerdict.ACCEPTED, 120L, elevenDaysAgo);
        Submission fail1 = createMockSubmission(fail1Id, userId, problemId, EvaluationVerdict.WRONG_ANSWER, 20L, eightDaysAgo);
        Submission fail2 = createMockSubmission(fail2Id, userId, problemId, EvaluationVerdict.COMPILE_ERROR, 0L, twelveDaysAgo);

        // findByCreatedAtBefore returns all 6 submissions older than 7 days
        when(submissionRepository.findByCreatedAtBefore(cutoff))
                .thenReturn(List.of(acc1, acc2, acc3, acc4, fail1, fail2));

        // find top accepted for this user and problem (sorted by execution time / created at)
        when(submissionRepository.findByUserIdAndProblemIdAndVerdictOrderByExecutionTimeMsAscCreatedAtDesc(
                eq(userId), eq(problemId), eq(EvaluationVerdict.ACCEPTED)))
                .thenReturn(List.of(acc1, acc2, acc3, acc4));

        int purgedCount = retentionService.purgeStaleSubmissions(cutoff, 3);

        // Should purge 3 items: acc4 (4th accepted), fail1, fail2
        assertThat(purgedCount).isEqualTo(3);

        verify(submissionHintRepository).deleteBySubmissionIdIn(deletedIdsCaptor.capture());
        Collection<UUID> deletedHintSubmissionIds = deletedIdsCaptor.getValue();
        assertThat(deletedHintSubmissionIds).containsExactlyInAnyOrder(acc4Id, fail1Id, fail2Id);

        verify(submissionRepository).deleteAllByIdIn(deletedIdsCaptor.capture());
        Collection<UUID> deletedSubIds = deletedIdsCaptor.getValue();
        assertThat(deletedSubIds).containsExactlyInAnyOrder(acc4Id, fail1Id, fail2Id);
    }

    @Test
    @DisplayName("Scheduled retention cleanup runs successfully and purges stale items")
    void scheduleRetentionCleanup_executesAndLogs() {
        when(submissionRepository.findByCreatedAtBefore(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        retentionService.scheduleRetentionCleanup();

        verify(submissionRepository).findByCreatedAtBefore(any(Instant.class));
    }
}
