package com.aicodingmentor.contest.repository;

import com.aicodingmentor.contest.domain.ContestParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContestParticipantRepository extends JpaRepository<ContestParticipant, UUID> {

    Optional<ContestParticipant> findByContestIdAndUserId(UUID contestId, UUID userId);

    List<ContestParticipant> findByContestIdOrderByTotalScoreDescTotalPenaltyMinutesAsc(UUID contestId);

    List<ContestParticipant> findByUserIdOrderByRegisteredAtDesc(UUID userId);

    long countByUserId(UUID userId);

    @Query("SELECT COUNT(p) FROM ContestParticipant p WHERE p.contest.id = :contestId")
    long countParticipantsByContestId(@Param("contestId") UUID contestId);
}
