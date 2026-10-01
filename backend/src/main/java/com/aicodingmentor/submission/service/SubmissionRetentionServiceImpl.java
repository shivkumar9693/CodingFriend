package com.aicodingmentor.submission.service;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.mentor.repository.SubmissionHintRepository;
import com.aicodingmentor.submission.domain.Submission;
import com.aicodingmentor.submission.repository.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of SubmissionRetentionService.
 * Enforces a 7-day retention window while safeguarding the top 2-3 accepted submissions
 * per user per problem.
 */
@Service
@Transactional
public class SubmissionRetentionServiceImpl implements SubmissionRetentionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionRetentionServiceImpl.class);

    private final SubmissionRepository submissionRepository;
    private final SubmissionHintRepository submissionHintRepository;
    private final int retentionDays;
    private final int topAcceptedToPreserve;

    public SubmissionRetentionServiceImpl(
            SubmissionRepository submissionRepository,
            SubmissionHintRepository submissionHintRepository,
            @Value("${app.submission.retention.days:7}") int retentionDays,
            @Value("${app.submission.retention.top-accepted-to-keep:3}") int topAcceptedToPreserve) {
        this.submissionRepository = Objects.requireNonNull(submissionRepository, "submissionRepository must not be null");
        this.submissionHintRepository = Objects.requireNonNull(submissionHintRepository, "submissionHintRepository must not be null");
        this.retentionDays = retentionDays;
        this.topAcceptedToPreserve = topAcceptedToPreserve;
    }

    @Override
    @Scheduled(cron = "${app.submission.retention.cron:0 0 2 * * ?}")
    public void scheduleRetentionCleanup() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        log.info("Starting automated submission retention cleanup. Cutoff: {} ({} days retention, preserving top {} accepted)",
                cutoff, retentionDays, topAcceptedToPreserve);

        int deletedCount = purgeStaleSubmissions(cutoff, topAcceptedToPreserve);
        log.info("Submission retention cleanup completed. Purged {} obsolete submissions.", deletedCount);
    }

    @Override
    public int purgeStaleSubmissions(Instant cutoffTime, int maxTopAcceptedToKeep) {
        Objects.requireNonNull(cutoffTime, "cutoffTime must not be null");

        // 1. Fetch candidate submissions older than the cutoff threshold
        List<Submission> oldSubmissions = submissionRepository.findByCreatedAtBefore(cutoffTime);
        if (oldSubmissions.isEmpty()) {
            log.debug("No stale submissions found older than {}", cutoffTime);
            return 0;
        }

        // 2. Determine unique (userId, problemId) pairs amongst candidate submissions
        Set<UserProblemKey> affectedPairs = oldSubmissions.stream()
                .map(s -> new UserProblemKey(s.getUserId(), s.getProblemId()))
                .collect(Collectors.toSet());

        // 3. For each affected pair, resolve and protect the top N ACCEPTED submissions (fastest runtime & newest)
        Set<UUID> protectedSubmissionIds = new HashSet<>();
        for (UserProblemKey key : affectedPairs) {
            List<Submission> acceptedSubmissions = submissionRepository
                    .findByUserIdAndProblemIdAndVerdictOrderByExecutionTimeMsAscCreatedAtDesc(
                            key.userId(), key.problemId(), EvaluationVerdict.ACCEPTED
                    );

            acceptedSubmissions.stream()
                    .limit(Math.max(1, maxTopAcceptedToKeep))
                    .map(Submission::getId)
                    .forEach(protectedSubmissionIds::add);
        }

        // 4. Identify submissions that are NOT protected
        List<UUID> idsToDelete = oldSubmissions.stream()
                .map(Submission::getId)
                .filter(id -> !protectedSubmissionIds.contains(id))
                .collect(Collectors.toList());

        if (idsToDelete.isEmpty()) {
            log.debug("All {} candidate submissions were protected top accepted solutions. 0 deleted.", oldSubmissions.size());
            return 0;
        }

        // 5. Clean up associated hints and remove stale submissions
        submissionHintRepository.deleteBySubmissionIdIn(idsToDelete);
        submissionRepository.deleteAllByIdIn(idsToDelete);

        log.info("Purged {} stale submissions (preserved {} top accepted submissions).",
                idsToDelete.size(), protectedSubmissionIds.size());

        return idsToDelete.size();
    }

    private record UserProblemKey(UUID userId, UUID problemId) {}
}
