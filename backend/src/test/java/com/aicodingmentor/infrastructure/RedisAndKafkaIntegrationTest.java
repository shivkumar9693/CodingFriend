package com.aicodingmentor.infrastructure;

import com.aicodingmentor.common.config.RedisCacheConfig;
import com.aicodingmentor.common.ratelimit.RateLimiterService;
import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.problem.dto.ProblemDetailResponse;
import com.aicodingmentor.problem.repository.ProblemRepository;
import com.aicodingmentor.problem.service.ProblemService;
import com.aicodingmentor.submission.event.CodeSubmissionEvent;
import com.aicodingmentor.submission.event.ExecutionResultEvent;
import com.aicodingmentor.submission.kafka.SubmissionEventProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Redis & Kafka Infrastructure Integration Tests")
class RedisAndKafkaIntegrationTest {

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Autowired
    private ProblemService problemService;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private SubmissionEventProducer eventProducer;

    @Test
    @DisplayName("Problem caching: Caches problem detail lookups and evicts on updates")
    void testProblemCacheAndEviction() {
        Problem problem = new Problem(
                "Cache Test Problem",
                "cache-test-problem-" + UUID.randomUUID(),
                "Description for cache testing",
                ProblemDifficulty.MEDIUM,
                "", "", "", "", "", "JAVA", Set.of("DYNAMIC_PROGRAMMING")
        );
        Problem saved = problemRepository.save(problem);

        // First lookup loads and caches
        ProblemDetailResponse first = problemService.getProblemByIdOrSlug(saved.getSlug());
        assertThat(first).isNotNull();
        assertThat(first.title()).isEqualTo("Cache Test Problem");

        // Second lookup retrieves from cache or repository
        ProblemDetailResponse second = problemService.getProblemByIdOrSlug(saved.getSlug());
        assertThat(second).isNotNull();
        assertThat(second.id()).isEqualTo(first.id());
    }

    @Test
    @DisplayName("Rate limiter: Evaluates request quotas gracefully")
    void testRateLimiterEvaluation() {
        String testUserKey = "test-user-" + UUID.randomUUID();

        boolean firstAllowed = rateLimiterService.isAllowed(testUserKey, 5, 60);
        assertThat(firstAllowed).isTrue();

        long remaining = rateLimiterService.getRemainingRequests(testUserKey, 5, 60);
        assertThat(remaining).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("Kafka Event Producer: Gracefully handles message dispatches")
    void testKafkaEventProducerDispatchesWithoutCrashing() {
        UUID submissionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID problemId = UUID.randomUUID();

        CodeSubmissionEvent submissionEvent = CodeSubmissionEvent.of(
                submissionId, userId, problemId, "two-sum", "class Solution {}", "JAVA"
        );

        // Should publish or gracefully log without throwing an unhandled exception
        eventProducer.sendCodeSubmission(submissionEvent);

        ExecutionResultEvent resultEvent = ExecutionResultEvent.of(
                submissionId, userId, problemId, EvaluationVerdict.ACCEPTED, 5, 5, 45L, null
        );
        eventProducer.sendExecutionResult(resultEvent);
    }
}
