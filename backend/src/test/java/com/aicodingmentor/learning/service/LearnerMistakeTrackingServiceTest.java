package com.aicodingmentor.learning.service;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.learning.domain.LearnerMistakeRecord;
import com.aicodingmentor.learning.dto.LearnerMistakeSummaryResponse;
import com.aicodingmentor.learning.dto.LearnerStatisticsResponse;
import com.aicodingmentor.learning.repository.LearnerMistakeRepository;
import com.aicodingmentor.mistake.model.MistakeType;
import com.aicodingmentor.submission.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LearnerMistakeTrackingService Unit Tests")
class LearnerMistakeTrackingServiceTest {

    @Mock
    private LearnerMistakeRepository mistakeRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private com.aicodingmentor.problem.repository.ProblemRepository problemRepository;

    @Mock
    private com.aicodingmentor.contest.repository.ContestParticipantRepository contestParticipantRepository;

    private LearnerMistakeTrackingServiceImpl trackingService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        trackingService = new LearnerMistakeTrackingServiceImpl(
                mistakeRepository,
                submissionRepository,
                problemRepository,
                contestParticipantRepository
        );
    }

    @Test
    @DisplayName("Should record new mistake pattern with initial frequency of 1")
    void testRecordNewMistake() {
        when(mistakeRepository.findFirstByUserIdAndMistakeType(userId, MistakeType.OFF_BY_ONE))
                .thenReturn(Optional.empty());

        trackingService.recordMistake(userId, MistakeType.OFF_BY_ONE, Set.of("ARRAY", "LOOPS"));

        ArgumentCaptor<LearnerMistakeRecord> captor = ArgumentCaptor.forClass(LearnerMistakeRecord.class);
        verify(mistakeRepository, times(1)).save(captor.capture());

        LearnerMistakeRecord saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getMistakeType()).isEqualTo(MistakeType.OFF_BY_ONE);
        assertThat(saved.getFrequency()).isEqualTo(1);
        assertThat(saved.getRelatedTopics()).contains("ARRAY", "LOOPS");
    }

    @Test
    @DisplayName("Should increment frequency and update topics for existing mistake")
    void testIncrementExistingMistake() {
        LearnerMistakeRecord existing = new LearnerMistakeRecord(userId, MistakeType.OFF_BY_ONE, Set.of("ARRAY"));
        when(mistakeRepository.findFirstByUserIdAndMistakeType(userId, MistakeType.OFF_BY_ONE))
                .thenReturn(Optional.of(existing));

        trackingService.recordMistake(userId, MistakeType.OFF_BY_ONE, Set.of("BINARY_SEARCH"));

        verify(mistakeRepository, times(1)).save(existing);
        assertThat(existing.getFrequency()).isEqualTo(2);
        assertThat(existing.getRelatedTopics()).contains("ARRAY", "BINARY_SEARCH");
    }

    @Test
    @DisplayName("Should compute aggregate learner statistics correctly")
    void testGetLearnerStatistics() {
        LearnerMistakeRecord r1 = new LearnerMistakeRecord(userId, MistakeType.OFF_BY_ONE, Set.of("ARRAY"));
        r1.incrementFrequency(Set.of());
        r1.incrementFrequency(Set.of()); // freq = 3

        LearnerMistakeRecord r2 = new LearnerMistakeRecord(userId, MistakeType.NULL_POINTER, Set.of("TREES")); // freq = 1

        when(submissionRepository.countByUserId(userId)).thenReturn(10L);
        when(submissionRepository.countByUserIdAndVerdict(userId, EvaluationVerdict.ACCEPTED)).thenReturn(6L);
        when(submissionRepository.countDistinctProblemsSolvedByUserId(userId)).thenReturn(4L);
        when(submissionRepository.countDistinctProblemsAttemptedByUserId(userId)).thenReturn(5L);
        when(mistakeRepository.findByUserIdOrderByFrequencyDesc(userId)).thenReturn(List.of(r1, r2));

        LearnerStatisticsResponse stats = trackingService.getLearnerStatistics(userId);

        assertThat(stats.totalSubmissions()).isEqualTo(10L);
        assertThat(stats.totalAccepted()).isEqualTo(6L);
        assertThat(stats.accuracyPercentage()).isEqualTo(60.0);
        assertThat(stats.problemsSolved()).isEqualTo(4L);
        assertThat(stats.problemsAttempted()).isEqualTo(5L);
        assertThat(stats.averageAttemptsPerProblem()).isEqualTo(2.0);
        assertThat(stats.mostFrequentMistake()).isEqualTo(MistakeType.OFF_BY_ONE);
        assertThat(stats.mistakeBreakdown().get(MistakeType.OFF_BY_ONE)).isEqualTo(3);
        assertThat(stats.mistakeBreakdown().get(MistakeType.NULL_POINTER)).isEqualTo(1);
        assertThat(stats.weakTopics().get("ARRAY")).isEqualTo(3);
        assertThat(stats.weakTopics().get("TREES")).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return list of learner mistakes")
    void testGetLearnerMistakes() {
        LearnerMistakeRecord r1 = new LearnerMistakeRecord(userId, MistakeType.OFF_BY_ONE, Set.of("ARRAY"));
        when(mistakeRepository.findByUserIdOrderByFrequencyDesc(userId)).thenReturn(List.of(r1));

        List<LearnerMistakeSummaryResponse> mistakes = trackingService.getLearnerMistakes(userId);

        assertThat(mistakes).hasSize(1);
        assertThat(mistakes.get(0).mistakeType()).isEqualTo(MistakeType.OFF_BY_ONE);
    }
}
