package com.aicodingmentor.contest.repository;

import com.aicodingmentor.contest.domain.Contest;
import com.aicodingmentor.contest.domain.ContestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContestRepository extends JpaRepository<Contest, UUID> {

    Optional<Contest> findBySlug(String slug);

    List<Contest> findAllByOrderByStartTimeDesc();

    List<Contest> findByStatusOrderByStartTimeAsc(ContestStatus status);

    @Query("SELECT c FROM Contest c WHERE c.startTime <= :now AND c.endTime >= :now ORDER BY c.endTime ASC")
    List<Contest> findLiveContests(@Param("now") Instant now);

    @Query("SELECT c FROM Contest c WHERE c.startTime > :now ORDER BY c.startTime ASC")
    List<Contest> findUpcomingContests(@Param("now") Instant now);

    @Query("SELECT c FROM Contest c WHERE c.endTime < :now ORDER BY c.endTime DESC")
    List<Contest> findPastContests(@Param("now") Instant now);
}
