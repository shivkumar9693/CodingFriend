package com.aicodingmentor.problem.repository;

import com.aicodingmentor.problem.domain.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {

    List<TestCase> findByProblemIdOrderBySequenceOrderAsc(UUID problemId);

    List<TestCase> findByProblemIdAndIsHiddenFalseOrderBySequenceOrderAsc(UUID problemId);

    java.util.Optional<TestCase> findByIdAndProblemId(UUID id, UUID problemId);

    void deleteByIdAndProblemId(UUID id, UUID problemId);
}
