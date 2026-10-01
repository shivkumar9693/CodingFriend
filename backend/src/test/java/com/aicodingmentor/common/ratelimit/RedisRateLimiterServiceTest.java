package com.aicodingmentor.common.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisRateLimiterService Unit Tests")
class RedisRateLimiterServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisRateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        rateLimiterService = new RedisRateLimiterService(redisTemplate);
    }

    @Test
    @DisplayName("Allows request when counter is below maximum quota")
    void testIsAllowed_UnderLimit_ReturnsTrue() {
        when(valueOperations.increment("ratelimit:submit:user1")).thenReturn(1L);

        boolean allowed = rateLimiterService.isAllowed("submit:user1", 30, 60);

        assertThat(allowed).isTrue();
        verify(redisTemplate).expire(eq("ratelimit:submit:user1"), eq(Duration.ofSeconds(60)));
    }

    @Test
    @DisplayName("Blocks request when counter exceeds maximum quota")
    void testIsAllowed_OverLimit_ReturnsFalse() {
        when(valueOperations.increment("ratelimit:submit:user1")).thenReturn(31L);

        boolean allowed = rateLimiterService.isAllowed("submit:user1", 30, 60);

        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("Fails open / in-memory fallback when Redis throws an exception")
    void testIsAllowed_RedisUnavailable_DegradesGracefully() {
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("Redis connection timed out"));

        boolean firstAttempt = rateLimiterService.isAllowed("submit:fallbackUser", 2, 60);
        boolean secondAttempt = rateLimiterService.isAllowed("submit:fallbackUser", 2, 60);
        boolean thirdAttempt = rateLimiterService.isAllowed("submit:fallbackUser", 2, 60);

        assertThat(firstAttempt).isTrue();
        assertThat(secondAttempt).isTrue();
        assertThat(thirdAttempt).isFalse(); // In-memory fallback enforces quota
    }

    @Test
    @DisplayName("Returns remaining requests correctly")
    void testGetRemainingRequests() {
        when(valueOperations.get("ratelimit:submit:user1")).thenReturn("12");

        long remaining = rateLimiterService.getRemainingRequests("submit:user1", 30, 60);

        assertThat(remaining).isEqualTo(18L);
    }
}
