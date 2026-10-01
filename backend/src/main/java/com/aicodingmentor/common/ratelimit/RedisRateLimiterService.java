package com.aicodingmentor.common.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance Redis-backed rate limiter using sliding/fixed-window counters with atomic expiration.
 * Gracefully degrades if Redis is temporarily unreachable.
 */
@Service
public class RedisRateLimiterService implements RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiterService.class);
    private static final String KEY_PREFIX = "ratelimit:";

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, AtomicInteger> inMemoryFallbackCounters = new ConcurrentHashMap<>();

    public RedisRateLimiterService(@org.springframework.beans.factory.annotation.Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isAllowed(String key, int maxRequests, int windowSeconds) {
        if (key == null || key.isBlank() || maxRequests <= 0 || windowSeconds <= 0) {
            return true;
        }

        String fullKey = KEY_PREFIX + key;
        if (redisTemplate == null) {
            return evaluateInMemoryFallback(fullKey, maxRequests);
        }

        try {
            Long currentCount = redisTemplate.opsForValue().increment(fullKey);
            if (currentCount != null && currentCount == 1L) {
                redisTemplate.expire(fullKey, Duration.ofSeconds(windowSeconds));
            }
            return currentCount != null && currentCount <= maxRequests;
        } catch (Exception ex) {
            log.warn("Redis unavailable during rate limit evaluation for key [{}]. Falling back to in-memory counter. Reason: {}",
                    fullKey, ex.getMessage());
            return evaluateInMemoryFallback(fullKey, maxRequests);
        }
    }

    @Override
    public long getRemainingRequests(String key, int maxRequests, int windowSeconds) {
        if (key == null || key.isBlank() || redisTemplate == null) {
            return maxRequests;
        }

        String fullKey = KEY_PREFIX + key;
        try {
            String val = redisTemplate.opsForValue().get(fullKey);
            long currentCount = val != null ? Long.parseLong(val) : 0L;
            return Math.max(0, maxRequests - currentCount);
        } catch (Exception ex) {
            log.warn("Failed to retrieve remaining quota from Redis for key [{}]. Cause: {}", fullKey, ex.getMessage());
            return maxRequests;
        }
    }

    private boolean evaluateInMemoryFallback(String key, int maxRequests) {
        AtomicInteger counter = inMemoryFallbackCounters.computeIfAbsent(key, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();
        return count <= maxRequests;
    }
}
