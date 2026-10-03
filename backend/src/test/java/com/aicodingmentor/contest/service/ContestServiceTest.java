package com.aicodingmentor.contest.service;

import com.aicodingmentor.contest.domain.Contest;
import com.aicodingmentor.contest.domain.ContestParticipant;
import com.aicodingmentor.contest.domain.ContestStatus;
import com.aicodingmentor.contest.dto.ContestDto;
import com.aicodingmentor.contest.dto.ContestLeaderboardDto;
import com.aicodingmentor.contest.dto.CreateContestRequest;
import com.aicodingmentor.contest.repository.ContestParticipantRepository;
import com.aicodingmentor.contest.repository.ContestProblemRepository;
import com.aicodingmentor.contest.repository.ContestRepository;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.domain.UserRole;
import com.aicodingmentor.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContestServiceTest {

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private ContestProblemRepository contestProblemRepository;

    @Mock
    private ContestParticipantRepository contestParticipantRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private ContestServiceImpl contestService;

    @BeforeEach
    void setUp() {
        contestService = new ContestServiceImpl(
                contestRepository,
                contestProblemRepository,
                contestParticipantRepository,
                problemRepository,
                userRepository,
                eventPublisher
        );
    }

    @Test
    @DisplayName("Should create contest with problems and calculate status")
    void shouldCreateContest() {
        UUID adminId = UUID.randomUUID();
        UUID problemId = UUID.randomUUID();
        Instant startTime = Instant.now().plus(Duration.ofHours(2));

        CreateContestRequest request = new CreateContestRequest(
                "Weekly Cup #1",
                "Description",
                startTime,
                90,
                List.of(new CreateContestRequest.ContestProblemSetup(problemId, 1, 500))
        );

        Problem mockProblem = new Problem();
        mockProblem.setId(problemId);
        mockProblem.setTitle("Two Sum");
        mockProblem.setSlug("two-sum");

        when(contestRepository.findBySlug(any())).thenReturn(Optional.empty());
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(mockProblem));
        when(contestRepository.save(any(Contest.class))).thenAnswer(inv -> inv.getArgument(0));

        ContestDto result = contestService.createContest(request, adminId);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Weekly Cup #1");
        assertThat(result.durationMinutes()).isEqualTo(90);
        assertThat(result.status()).isEqualTo(ContestStatus.UPCOMING);
        verify(contestRepository).save(any(Contest.class));
    }

    @Test
    @DisplayName("Should register user for contest")
    void shouldRegisterForContest() {
        UUID userId = UUID.randomUUID();
        String slug = "weekly-cup-1";

        User mockUser = new User("learner@test.com", "hash", "Alex", UserRole.LEARNER);
        mockUser.setId(userId);

        Contest mockContest = new Contest(
                "Weekly Cup #1",
                slug,
                "Desc",
                Instant.now().plus(Duration.ofHours(1)),
                Instant.now().plus(Duration.ofHours(3)),
                120,
                UUID.randomUUID()
        );
        mockContest.setId(UUID.randomUUID());

        when(contestRepository.findBySlug(slug)).thenReturn(Optional.of(mockContest));
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(contestParticipantRepository.findByContestIdAndUserId(mockContest.getId(), userId)).thenReturn(Optional.empty());
        when(contestParticipantRepository.save(any(ContestParticipant.class))).thenAnswer(inv -> inv.getArgument(0));

        var participantDto = contestService.registerForContest(slug, userId);

        assertThat(participantDto).isNotNull();
        assertThat(participantDto.fullName()).isEqualTo("Alex");
        verify(contestParticipantRepository).save(any(ContestParticipant.class));
    }

    @Test
    @DisplayName("Should return ranked leaderboard")
    void shouldReturnLeaderboard() {
        String slug = "weekly-cup-1";
        Contest mockContest = new Contest();
        mockContest.setId(UUID.randomUUID());
        mockContest.setTitle("Weekly Cup #1");
        mockContest.setStatus(ContestStatus.LIVE);

        User user1 = new User("u1@test.com", "h", "Player One", UserRole.LEARNER);
        user1.setId(UUID.randomUUID());
        ContestParticipant p1 = new ContestParticipant(mockContest, user1);
        p1.setTotalScore(1000);
        p1.setTotalPenaltyMinutes(30);

        when(contestRepository.findBySlug(slug)).thenReturn(Optional.of(mockContest));
        when(contestParticipantRepository.findByContestIdOrderByTotalScoreDescTotalPenaltyMinutesAsc(mockContest.getId()))
                .thenReturn(List.of(p1));

        ContestLeaderboardDto lb = contestService.getLeaderboard(slug);

        assertThat(lb).isNotNull();
        assertThat(lb.totalParticipants()).isEqualTo(1);
        assertThat(lb.standings().get(0).fullName()).isEqualTo("Player One");
        assertThat(lb.standings().get(0).rank()).isEqualTo(1);
    }
}
