package com.aicodingmentor.submission.repository;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.submission.domain.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    Page<Submission> findByUserId(UUID userId, Pageable pageable);

    Page<Submission> findByUserIdAndProblemId(UUID userId, UUID problemId, Pageable pageable);

    Page<Submission> findByProblemId(UUID problemId, Pageable pageable);

    long countByUserId(UUID userId);

    long countByUserIdAndVerdict(UUID userId, EvaluationVerdict verdict);

    @Query("SELECT COUNT(DISTINCT s.problemId) FROM Submission s WHERE s.userId = :userId AND s.verdict = 'ACCEPTED'")
    long countDistinctProblemsSolvedByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(DISTINCT s.problemId) FROM Submission s WHERE s.userId = :userId")
    long countDistinctProblemsAttemptedByUserId(@Param("userId") UUID userId);

    List<Submission> findByCreatedAtBefore(Instant cutoffTime);

    List<Submission> findByUserIdAndProblemIdAndVerdictOrderByExecutionTimeMsAscCreatedAtDesc(
            UUID userId, UUID problemId, EvaluationVerdict verdict);

    @Modifying
    @Query("DELETE FROM Submission s WHERE s.id IN :ids")
    void deleteAllByIdIn(@Param("ids") Collection<UUID> ids);
}

