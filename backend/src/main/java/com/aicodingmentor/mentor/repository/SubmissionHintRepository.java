package com.aicodingmentor.mentor.repository;

import com.aicodingmentor.mentor.domain.SubmissionHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionHintRepository extends JpaRepository<SubmissionHint, UUID> {

    List<SubmissionHint> findBySubmissionIdOrderByHintLevelAsc(UUID submissionId);

    Optional<SubmissionHint> findFirstBySubmissionIdAndHintLevelOrderByCreatedAtDesc(UUID submissionId, int hintLevel);

    List<SubmissionHint> findBySubmissionIdAndHintLevel(UUID submissionId, int hintLevel);

    List<SubmissionHint> findByUserIdAndProblemIdOrderByCreatedAtDesc(UUID userId, UUID problemId);

    void deleteBySubmissionId(UUID submissionId);

    @Modifying
    @Query("DELETE FROM SubmissionHint h WHERE h.submissionId IN :submissionIds")
    void deleteBySubmissionIdIn(@Param("submissionIds") Collection<UUID> submissionIds);
}

