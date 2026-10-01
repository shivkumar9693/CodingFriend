package com.aicodingmentor.problem.repository;

import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ProblemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProblemRepository problemRepository;

    @Test
    @DisplayName("Specification filters problems by difficulty and tag correctly")
    void testFilterByDifficultyAndTag() {
        Problem p1 = new Problem("Easy Array", "easy-array", "Desc 1", ProblemDifficulty.EASY, null, null, null, null, null, "JAVA", Set.of("ARRAY", "TWO_POINTERS"));
        Problem p2 = new Problem("Hard Array", "hard-array", "Desc 2", ProblemDifficulty.HARD, null, null, null, null, null, "JAVA", Set.of("ARRAY"));
        Problem p3 = new Problem("Easy Tree", "easy-tree", "Desc 3", ProblemDifficulty.EASY, null, null, null, null, null, "JAVA", Set.of("TREE"));

        entityManager.persist(p1);
        entityManager.persist(p2);
        entityManager.persist(p3);
        entityManager.flush();

        Specification<Problem> spec = ProblemSpecifications.filterBy(ProblemDifficulty.EASY, "ARRAY");
        Page<Problem> page = problemRepository.findAll(spec, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("easy-array", page.getContent().get(0).getSlug());
    }

    @Test
    @DisplayName("findBySlug returns existing problem")
    void testFindBySlug() {
        Problem p = new Problem("Slug Test", "unique-slug-123", "Desc", ProblemDifficulty.MEDIUM, null, null, null, null, null, "JAVA", Set.of("MATH"));
        entityManager.persist(p);
        entityManager.flush();

        assertTrue(problemRepository.existsBySlug("unique-slug-123"));
        assertTrue(problemRepository.findBySlug("unique-slug-123").isPresent());
    }
}
