package com.aicodingmentor.contest.repository;

import com.aicodingmentor.contest.domain.ContestProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContestProblemRepository extends JpaRepository<ContestProblem, UUID> {

    List<ContestProblem> findByContestIdOrderByProblemOrderAsc(UUID contestId);

    Optional<ContestProblem> findByContestIdAndProblemId(UUID contestId, UUID problemId);
}
