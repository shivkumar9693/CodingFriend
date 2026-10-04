package com.aicodingmentor.duel.service;

import com.aicodingmentor.common.error.ApiException;
import com.aicodingmentor.common.error.ResourceNotFoundException;
import com.aicodingmentor.duel.domain.Duel;
import com.aicodingmentor.duel.domain.DuelStatus;
import com.aicodingmentor.duel.dto.CreateDuelRequest;
import com.aicodingmentor.duel.dto.DuelDto;
import com.aicodingmentor.duel.repository.DuelRepository;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class DuelServiceImpl implements DuelService {

    private static final String ROOM_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Random RANDOM = new SecureRandom();

    private final DuelRepository duelRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public DuelServiceImpl(
            DuelRepository duelRepository,
            ProblemRepository problemRepository,
            UserRepository userRepository
    ) {
        this(duelRepository, problemRepository, userRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DuelServiceImpl(
            DuelRepository duelRepository,
            ProblemRepository problemRepository,
            UserRepository userRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate
    ) {
        this.duelRepository = duelRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public DuelDto createOrQueueDuel(CreateDuelRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // If matchmaking queue mode without specific problem, check if an open duel exists
        if (request.problemId() == null) {
            List<Duel> openDuels = duelRepository.findOpenQueueDuels(request.difficulty(), userId);
            if (!openDuels.isEmpty()) {
                Duel existingDuel = openDuels.get(0);
                return joinDuelInternal(existingDuel, user);
            }
        }

        // Determine problem for duel
        Problem problem;
        if (request.problemId() != null) {
            problem = problemRepository.findById(request.problemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Problem", request.problemId()));
        } else {
            ProblemDifficulty diff = ProblemDifficulty.valueOf(request.difficulty());
            List<Problem> problems = problemRepository.findByDifficulty(diff);
            if (problems.isEmpty()) {
                problems = problemRepository.findAll();
            }
            if (problems.isEmpty()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "No problems available to create a duel");
            }
            problem = problems.get(RANDOM.nextInt(problems.size()));
        }

        String roomCode = generateRoomCode();
        Duel duel = new Duel(roomCode, user, problem, request.difficulty());
        Duel saved = duelRepository.save(duel);
        return DuelDto.from(saved);
    }

    @Override
    public DuelDto joinDuelRoom(String roomCode, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Duel duel = duelRepository.findByRoomCode(roomCode.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Duel room", roomCode));

        return joinDuelInternal(duel, user);
    }

    private DuelDto joinDuelInternal(Duel duel, User user) {
        if (duel.getPlayer1().getId().equals(user.getId())) {
            return DuelDto.from(duel);
        }

        if (duel.getStatus() != DuelStatus.WAITING) {
            if (duel.getPlayer2() != null && duel.getPlayer2().getId().equals(user.getId())) {
                return DuelDto.from(duel);
            }
            throw new ApiException(HttpStatus.BAD_REQUEST, "Duel room is already full or in progress");
        }

        duel.setPlayer2(user);
        duel.setStatus(DuelStatus.IN_PROGRESS);
        duel.setStartTime(Instant.now());
        duel.setEndTime(Instant.now().plus(Duration.ofMinutes(15)));

        Duel saved = duelRepository.save(duel);
        DuelDto dto = DuelDto.from(saved);
        broadcastDuelUpdate(dto);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public DuelDto getDuelByRoomCode(String roomCode) {
        Duel duel = duelRepository.findByRoomCode(roomCode.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Duel room", roomCode));
        return DuelDto.from(duel);
    }

    @Override
    public DuelDto updateDuelProgress(String roomCode, UUID userId, int progressPercent) {
        Duel duel = duelRepository.findByRoomCode(roomCode.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Duel room", roomCode));

        if (duel.getPlayer1().getId().equals(userId)) {
            duel.setPlayer1Progress(Math.min(100, Math.max(0, progressPercent)));
        } else if (duel.getPlayer2() != null && duel.getPlayer2().getId().equals(userId)) {
            duel.setPlayer2Progress(Math.min(100, Math.max(0, progressPercent)));
        }

        Duel saved = duelRepository.save(duel);
        DuelDto dto = DuelDto.from(saved);
        broadcastDuelUpdate(dto);
        return dto;
    }

    @Override
    public DuelDto completeDuel(String roomCode, UUID winnerId) {
        Duel duel = duelRepository.findByRoomCode(roomCode.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Duel room", roomCode));

        if (duel.getStatus() == DuelStatus.COMPLETED) {
            return DuelDto.from(duel);
        }

        User winner = userRepository.findById(winnerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", winnerId));

        duel.setWinner(winner);
        duel.setStatus(DuelStatus.COMPLETED);
        duel.setEndTime(Instant.now());

        // Update ELO rating (+25 for winner, -20 for loser)
        User loser = null;
        if (duel.getPlayer1().getId().equals(winnerId)) {
            loser = duel.getPlayer2();
        } else {
            loser = duel.getPlayer1();
        }

        winner.setDuelRating(winner.getDuelRating() + 25);
        userRepository.save(winner);

        if (loser != null) {
            loser.setDuelRating(Math.max(100, loser.getDuelRating() - 20));
            userRepository.save(loser);
        }

        Duel saved = duelRepository.save(duel);
        DuelDto dto = DuelDto.from(saved);
        broadcastDuelUpdate(dto);
        return dto;
    }

    private void broadcastDuelUpdate(DuelDto dto) {
        if (messagingTemplate != null && dto != null && dto.roomCode() != null) {
            try {
                messagingTemplate.convertAndSend("/topic/duel/" + dto.roomCode().toUpperCase(), dto);
            } catch (Exception ignored) {
                // Silently ignore broadcast failure to maintain 100% resilience
            }
        }
    }

    @Override
    public void leaveOrCancelDuel(String roomCode, UUID userId) {
        Duel duel = duelRepository.findByRoomCode(roomCode.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Duel room", roomCode));

        if (duel.getStatus() == DuelStatus.WAITING) {
            // If creator cancels waiting room, delete it immediately so it doesn't take DB space
            if (duel.getPlayer1().getId().equals(userId)) {
                duelRepository.delete(duel);
            }
        } else if (duel.getStatus() == DuelStatus.IN_PROGRESS) {
            // If player forfeits an in-progress match, award victory to opponent and finalize
            User winner = duel.getPlayer1().getId().equals(userId) ? duel.getPlayer2() : duel.getPlayer1();
            if (winner != null) {
                completeDuel(roomCode, winner.getId());
            }
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 1800000)
    public void scheduledCleanup() {
        cleanupOldDuels(1);
    }

    @Override
    public int cleanupOldDuels(int olderThanHours) {
        int hours = olderThanHours > 0 ? olderThanHours : 1;
        Instant cutoff = Instant.now().minus(Duration.ofHours(hours));
        return duelRepository.deleteOldCompletedDuels(cutoff);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DuelDto> getUserDuelHistory(UUID userId) {
        return duelRepository.findUserDuelHistory(userId)
                .stream()
                .map(DuelDto::from)
                .collect(Collectors.toList());
    }

    private String generateRoomCode() {
        StringBuilder sb = new StringBuilder("DUEL-");
        for (int i = 0; i < 5; i++) {
            sb.append(ROOM_CODE_CHARS.charAt(RANDOM.nextInt(ROOM_CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
