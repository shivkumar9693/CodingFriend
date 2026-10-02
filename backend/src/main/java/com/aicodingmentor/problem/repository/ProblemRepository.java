package com.aicodingmentor.problem.repository;

import com.aicodingmentor.problem.domain.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, UUID>, JpaSpecificationExecutor<Problem> {

    Optional<Problem> findBySlug(String slug);

    java.util.List<Problem> findByDifficulty(com.aicodingmentor.problem.domain.ProblemDifficulty difficulty);

    long countByDifficulty(com.aicodingmentor.problem.domain.ProblemDifficulty difficulty);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);
}
