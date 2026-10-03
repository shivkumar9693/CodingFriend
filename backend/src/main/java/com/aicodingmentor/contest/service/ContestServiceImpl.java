package com.aicodingmentor.contest.service;

import com.aicodingmentor.common.config.RedisCacheConfig;
import com.aicodingmentor.common.error.ApiException;
import com.aicodingmentor.common.error.ResourceNotFoundException;
import com.aicodingmentor.contest.domain.Contest;
import com.aicodingmentor.contest.domain.ContestParticipant;
import com.aicodingmentor.contest.domain.ContestProblem;
import com.aicodingmentor.contest.domain.ContestStatus;
import com.aicodingmentor.contest.dto.ContestDto;
import com.aicodingmentor.contest.dto.ContestLeaderboardDto;
import com.aicodingmentor.contest.dto.ContestParticipantDto;
import com.aicodingmentor.contest.dto.CreateContestRequest;
import com.aicodingmentor.contest.repository.ContestParticipantRepository;
import com.aicodingmentor.contest.repository.ContestProblemRepository;
import com.aicodingmentor.contest.repository.ContestRepository;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ContestServiceImpl implements ContestService {

    private final ContestRepository contestRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ContestParticipantRepository contestParticipantRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public ContestServiceImpl(
            ContestRepository contestRepository,
            ContestProblemRepository contestProblemRepository,
            ContestParticipantRepository contestParticipantRepository,
            ProblemRepository problemRepository,
            UserRepository userRepository,
            org.springframework.context.ApplicationEventPublisher eventPublisher
    ) {
        this.contestRepository = contestRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.contestParticipantRepository = contestParticipantRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContestDto> getAllContests(UUID currentUserId) {
        List<Contest> contests = contestRepository.findAllByOrderByStartTimeDesc();
        return contests.stream().map(c -> {
            long count = contestParticipantRepository.countParticipantsByContestId(c.getId());
            boolean isRegistered = currentUserId != null &&
                    contestParticipantRepository.findByContestIdAndUserId(c.getId(), currentUserId).isPresent();
            return ContestDto.from(c, count, isRegistered);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ContestDto getContestBySlug(String slug, UUID currentUserId) {
        Contest contest = contestRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Contest", slug));

        long count = contestParticipantRepository.countParticipantsByContestId(contest.getId());
        boolean isRegistered = currentUserId != null &&
                contestParticipantRepository.findByContestIdAndUserId(contest.getId(), currentUserId).isPresent();

        return ContestDto.from(contest, count, isRegistered);
    }

    @Override
    public ContestDto createContest(CreateContestRequest request, UUID adminUserId) {
        String slug = generateSlug(request.title());
        if (contestRepository.findBySlug(slug).isPresent()) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 6);
        }

        Instant endTime = request.startTime().plus(Duration.ofMinutes(request.durationMinutes()));
        Contest contest = new Contest(
                request.title(),
                slug,
                request.description(),
                request.startTime(),
                endTime,
                request.durationMinutes(),
                adminUserId
        );

        if (request.problems() != null) {
            for (CreateContestRequest.ContestProblemSetup pSetup : request.problems()) {
                Problem problem = problemRepository.findById(pSetup.problemId())
                        .orElseThrow(() -> new ResourceNotFoundException("Problem", pSetup.problemId()));
                ContestProblem cp = new ContestProblem(contest, problem, pSetup.problemOrder(), pSetup.points());
                contest.addProblem(cp);
            }
        }

        Contest saved = contestRepository.save(contest);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new com.aicodingmentor.notification.event.ContestCreatedNotificationEvent(
                    saved.getId(),
                    saved.getTitle(),
                    saved.getStartTime(),
                    saved.getDurationMinutes()
            ));
        }

        return ContestDto.from(saved, 0, false);
    }

    @Override
    public ContestParticipantDto registerForContest(String slug, UUID currentUserId) {
        if (currentUserId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required to register for contest");
        }

        Contest contest = contestRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Contest", slug));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        if (contest.getStatus() == ContestStatus.ENDED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot register for a contest that has already ended");
        }

        ContestParticipant participant = contestParticipantRepository.findByContestIdAndUserId(contest.getId(), currentUserId)
                .orElseGet(() -> {
                    ContestParticipant p = new ContestParticipant(contest, user);
                    return contestParticipantRepository.save(p);
                });

        return ContestParticipantDto.from(participant, 0);
    }

    @Override
    @Cacheable(value = RedisCacheConfig.CACHE_CONTESTS, key = "'leaderboard_' + #slug", unless = "#result == null")
    @Transactional(readOnly = true)
    public ContestLeaderboardDto getLeaderboard(String slug) {
        Contest contest = contestRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Contest", slug));

        List<ContestParticipant> participants = contestParticipantRepository
                .findByContestIdOrderByTotalScoreDescTotalPenaltyMinutesAsc(contest.getId());

        List<ContestParticipantDto> standings = new ArrayList<>();
        int currentRank = 1;
        for (ContestParticipant p : participants) {
            standings.add(ContestParticipantDto.from(p, currentRank++));
        }

        return new ContestLeaderboardDto(
                contest.getId(),
                contest.getTitle(),
                contest.getStatus().name(),
                standings.size(),
                standings
        );
    }

    @Override
    public void recordTabSwitch(String slug, UUID currentUserId) {
        if (currentUserId == null) return;
        Contest contest = contestRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Contest", slug));

        contestParticipantRepository.findByContestIdAndUserId(contest.getId(), currentUserId)
                .ifPresent(p -> {
                    p.incrementTabSwitch();
                    contestParticipantRepository.save(p);
                });
    }

    @Override
    @CacheEvict(value = RedisCacheConfig.CACHE_CONTESTS, allEntries = true)
    public void recordSuccessfulProblemSubmission(UUID contestId, UUID problemId, UUID userId, int minutesFromStart) {
        if (contestId == null || problemId == null || userId == null) return;

        contestProblemRepository.findByContestIdAndProblemId(contestId, problemId).ifPresent(cp -> {
            contestParticipantRepository.findByContestIdAndUserId(contestId, userId).ifPresent(participant -> {
                participant.setTotalScore(participant.getTotalScore() + cp.getPoints());
                participant.setTotalPenaltyMinutes(participant.getTotalPenaltyMinutes() + Math.max(0, minutesFromStart));
                contestParticipantRepository.save(participant);
            });
        });
    }

    private String generateSlug(String title) {
        if (title == null) return "contest-" + UUID.randomUUID().toString().substring(0, 8);
        return title.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }
}
