package com.aicodingmentor.submission.service;

import java.time.Instant;

/**
 * Service responsible for retention management and cleanup of historical code submissions.
 * Retains the top N accepted solutions per user per problem and purges stale/failing
 * submissions after a configurable retention window (e.g. 7 days).
 */
public interface SubmissionRetentionService {

    /**
     * Purges stale submissions older than the specified cutoff timestamp,
     * while unconditionally preserving the top N best-performing accepted submissions
     * for every user and problem.
     *
     * @param cutoffTime submissions created before this timestamp are candidates for cleanup
     * @param maxTopAcceptedToKeep number of top accepted submissions to exempt from deletion
     * @return total count of deleted submissions
     */
    int purgeStaleSubmissions(Instant cutoffTime, int maxTopAcceptedToKeep);

    /**
     * Scheduled automated task executing daily to enforce the 7-day retention policy.
     */
    void scheduleRetentionCleanup();
}
