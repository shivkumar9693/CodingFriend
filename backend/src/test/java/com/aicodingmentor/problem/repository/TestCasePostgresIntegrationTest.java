package com.aicodingmentor.problem.repository;

import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.problem.domain.TestCase;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class TestCasePostgresIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ai_coding_mentor_testcases")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
            registry.add("spring.flyway.enabled", () -> "true");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        }
    }

    @Autowired(required = false)
    private Flyway flyway;

    @Autowired(required = false)
    private ProblemRepository problemRepository;

    @Autowired(required = false)
    private TestCaseRepository testCaseRepository;

    @Test
    @DisplayName("Verify PostgreSQL container executes V4 migration and persists/queries problem test suites")
    void testPostgresTestCasePersistenceAndQuerying() {
        if (!postgres.isRunning()) {
            return;
        }

        assertNotNull(flyway, "Flyway bean must be present and initialized");
        assertNotNull(problemRepository, "ProblemRepository must be initialized");
        assertNotNull(testCaseRepository, "TestCaseRepository must be initialized");

        Problem problem = new Problem(
                "Binary Search",
                "binary-search",
                "Search in sorted array",
                ProblemDifficulty.EASY,
                "1 <= n <= 10^4",
                "Sorted array nums, target int",
                "Index of target or -1",
                "Example 1",
                "class Solution {}",
                "JAVA",
                Set.of("ARRAY", "BINARY_SEARCH")
        );

        TestCase sampleTest = new TestCase("[-1, 0, 3, 5, 9, 12]\n9", "4", false, 1, "Target 9 exists at index 4");
        TestCase hiddenTest = new TestCase("[-1, 0, 3, 5, 9, 12]\n2", "-1", true, 2, null, "{\"edge\":true}");

        problem.addTestCase(sampleTest);
        problem.addTestCase(hiddenTest);

        Problem savedProblem = problemRepository.save(problem);
        assertNotNull(savedProblem.getId());

        List<TestCase> visibleCases = testCaseRepository.findByProblemIdAndIsHiddenFalseOrderBySequenceOrderAsc(savedProblem.getId());
        assertEquals(1, visibleCases.size());
        assertEquals("4", visibleCases.get(0).getExpectedOutput());
        assertFalse(visibleCases.get(0).isHidden());

        List<TestCase> allCases = testCaseRepository.findByProblemIdOrderBySequenceOrderAsc(savedProblem.getId());
        assertEquals(2, allCases.size());
    }
}
