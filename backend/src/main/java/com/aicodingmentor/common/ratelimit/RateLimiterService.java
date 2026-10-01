package com.aicodingmentor.common.ratelimit;

/**
 * Service interface for rate limiting operations.
 */
public interface RateLimiterService {

    /**
     * Checks if a request identified by key is permitted under the rate limit policy.
     *
     * @param key unique identifier (e.g., "submit:user-uuid" or "hint:ip-address")
     * @param maxRequests maximum allowed requests within the time window
     * @param windowSeconds time window duration in seconds
     * @return true if the request is allowed; false if rate limit is exceeded
     */
    boolean isAllowed(String key, int maxRequests, int windowSeconds);

    /**
     * Gets remaining request quota in the current window.
     */
    long getRemainingRequests(String key, int maxRequests, int windowSeconds);
}
