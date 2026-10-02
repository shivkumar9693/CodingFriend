package com.aicodingmentor.learning.repository;

import com.aicodingmentor.learning.domain.MasteryStatus;
import com.aicodingmentor.learning.domain.RevisionNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RevisionNoteRepository extends JpaRepository<RevisionNote, UUID> {

    List<RevisionNote> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<RevisionNote> findByIdAndUserId(UUID id, UUID userId);

    List<RevisionNote> findByUserIdAndMasteryStatusOrderByCreatedAtDesc(UUID userId, MasteryStatus masteryStatus);

    List<RevisionNote> findByUserIdAndIsBookmarkedTrueOrderByCreatedAtDesc(UUID userId);

    List<RevisionNote> findByUserIdAndProblemIdOrderByCreatedAtDesc(UUID userId, UUID problemId);

    @Query("SELECT r FROM RevisionNote r WHERE r.userId = :userId AND " +
           "(LOWER(r.problemTitle) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(r.conceptSummary) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(r.keyTakeaway) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY r.createdAt DESC")
    List<RevisionNote> searchUserNotes(@Param("userId") UUID userId, @Param("query") String query);

    long countByUserId(UUID userId);

    long countByUserIdAndMasteryStatus(UUID userId, MasteryStatus masteryStatus);

    long countByUserIdAndIsBookmarkedTrue(UUID userId);

    @Query("SELECT COUNT(r) FROM RevisionNote r WHERE r.userId = :userId AND r.nextReviewAt IS NOT NULL AND r.nextReviewAt <= :now")
    long countDueForReview(@Param("userId") UUID userId, @Param("now") Instant now);
}
