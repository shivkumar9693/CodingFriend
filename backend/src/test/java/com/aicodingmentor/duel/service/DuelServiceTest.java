package com.aicodingmentor.duel.service;

import com.aicodingmentor.duel.domain.Duel;
import com.aicodingmentor.duel.domain.DuelStatus;
import com.aicodingmentor.duel.dto.CreateDuelRequest;
import com.aicodingmentor.duel.dto.DuelDto;
import com.aicodingmentor.duel.repository.DuelRepository;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DuelServiceTest {

    @Mock
    private DuelRepository duelRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private UserRepository userRepository;

    private DuelServiceImpl duelService;

    @BeforeEach
    void setUp() {
        duelService = new DuelServiceImpl(duelRepository, problemRepository, userRepository);
    }

    @Test
    @DisplayName("Should create new waiting duel room when queue is empty")
    void shouldCreateDuelRoom() {
        UUID userId = UUID.randomUUID();
        User mockUser = new User("u@test.com", "h", "Player 1", UserRole.LEARNER);
        mockUser.setId(userId);

        Problem mockProblem = new Problem();
        mockProblem.setId(UUID.randomUUID());
        mockProblem.setTitle("Two Sum");
        mockProblem.setSlug("two-sum");

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(duelRepository.findOpenQueueDuels("EASY", userId)).thenReturn(Collections.emptyList());
        when(problemRepository.findByDifficulty(ProblemDifficulty.EASY)).thenReturn(List.of(mockProblem));
        when(duelRepository.save(any(Duel.class))).thenAnswer(inv -> inv.getArgument(0));

        DuelDto dto = duelService.createOrQueueDuel(new CreateDuelRequest("EASY", null), userId);

        assertThat(dto).isNotNull();
        assertThat(dto.player1Name()).isEqualTo("Player 1");
        assertThat(dto.status()).isEqualTo(DuelStatus.WAITING);
        verify(duelRepository).save(any(Duel.class));
    }

    @Test
    @DisplayName("Should pair player2 and start duel when joining existing waiting room")
    void shouldJoinAndStartDuel() {
        UUID p1Id = UUID.randomUUID();
        UUID p2Id = UUID.randomUUID();

        User player1 = new User("p1@test.com", "h", "Player 1", UserRole.LEARNER);
        player1.setId(p1Id);

        User player2 = new User("p2@test.com", "h", "Player 2", UserRole.LEARNER);
        player2.setId(p2Id);

        Problem mockProblem = new Problem();
        mockProblem.setId(UUID.randomUUID());
        mockProblem.setTitle("Valid Palindrome");

        Duel waitingDuel = new Duel("DUEL-ABC12", player1, mockProblem, "EASY");

        when(userRepository.findById(p2Id)).thenReturn(Optional.of(player2));
        when(duelRepository.findByRoomCode("DUEL-ABC12")).thenReturn(Optional.of(waitingDuel));
        when(duelRepository.save(any(Duel.class))).thenAnswer(inv -> inv.getArgument(0));

        DuelDto result = duelService.joinDuelRoom("DUEL-ABC12", p2Id);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(DuelStatus.IN_PROGRESS);
        assertThat(result.player2Name()).isEqualTo("Player 2");
        assertThat(result.startTime()).isNotNull();
    }

    @Test
    @DisplayName("Should complete duel and adjust ELO ratings")
    void shouldCompleteDuelAndAdjustElo() {
        UUID p1Id = UUID.randomUUID();
        UUID p2Id = UUID.randomUUID();

        User player1 = new User("p1@test.com", "h", "Player 1", UserRole.LEARNER);
        player1.setId(p1Id);
        player1.setDuelRating(1200);

        User player2 = new User("p2@test.com", "h", "Player 2", UserRole.LEARNER);
        player2.setId(p2Id);
        player2.setDuelRating(1200);

        Problem mockProblem = new Problem();
        mockProblem.setId(UUID.randomUUID());

        Duel inProgressDuel = new Duel("DUEL-XYZ88", player1, mockProblem, "EASY");
        inProgressDuel.setPlayer2(player2);
        inProgressDuel.setStatus(DuelStatus.IN_PROGRESS);

        when(duelRepository.findByRoomCode("DUEL-XYZ88")).thenReturn(Optional.of(inProgressDuel));
        when(userRepository.findById(p1Id)).thenReturn(Optional.of(player1));
        when(duelRepository.save(any(Duel.class))).thenAnswer(inv -> inv.getArgument(0));

        DuelDto completed = duelService.completeDuel("DUEL-XYZ88", p1Id);

        assertThat(completed).isNotNull();
        assertThat(completed.status()).isEqualTo(DuelStatus.COMPLETED);
        assertThat(player1.getDuelRating()).isEqualTo(1225); // +25 ELO
        assertThat(player2.getDuelRating()).isEqualTo(1180); // -20 ELO
    }
}
