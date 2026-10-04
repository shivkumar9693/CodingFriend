package com.aicodingmentor.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Production-ready Redis Caching configuration for problem catalog and submission status.
 * Enforces graceful degradation: if Redis is unreachable or throws a connection error,
 * the application continues operating seamlessly via direct database lookups.
 */
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@EnableCaching
@ConditionalOnClass(name = "org.springframework.data.redis.connection.RedisConnectionFactory")
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class RedisCacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);

    public static final String CACHE_PROBLEMS = "problems";
    public static final String CACHE_PROBLEM_DETAILS = "problem-details";
    public static final String CACHE_SUBMISSION_STATUS = "submission-status";
    public static final String CACHE_LEADERBOARD = "leaderboard";
    public static final String CACHE_CONTESTS = "contests";
    public static final String CACHE_USER_AUTH_EMAIL = "user-auth-email";
    public static final String CACHE_USER_AUTH_ID = "user-auth-id";
    public static final String CACHE_USER_PROFILE = "user-profile";
    public static final String CACHE_USER_ACTIVITY = "user-activity";

    @Bean
    public RedisCacheConfiguration defaultCacheConfiguration() {
        ObjectMapper cacheObjectMapper = new ObjectMapper();
        cacheObjectMapper.registerModule(new JavaTimeModule());
        cacheObjectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        cacheObjectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(cacheObjectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("v2:")
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(RedisCacheConfiguration defaultCacheConfiguration) {
        return builder -> builder
                .withCacheConfiguration(CACHE_PROBLEMS,
                        defaultCacheConfiguration.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration(CACHE_PROBLEM_DETAILS,
                        defaultCacheConfiguration.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration(CACHE_SUBMISSION_STATUS,
                        defaultCacheConfiguration.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration(CACHE_LEADERBOARD,
                        defaultCacheConfiguration.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration(CACHE_CONTESTS,
                        defaultCacheConfiguration.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration(CACHE_USER_AUTH_EMAIL,
                        defaultCacheConfiguration.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration(CACHE_USER_AUTH_ID,
                        defaultCacheConfiguration.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration(CACHE_USER_PROFILE,
                        defaultCacheConfiguration.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration(CACHE_USER_ACTIVITY,
                        defaultCacheConfiguration.entryTtl(Duration.ofMinutes(5)));
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis GET failure for cache [{}] with key [{}]. Falling back to direct database retrieval. Cause: {}",
                        cache != null ? cache.getName() : "unknown", key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis PUT failure for cache [{}] with key [{}]. Cache write bypassed. Cause: {}",
                        cache != null ? cache.getName() : "unknown", key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis EVICT failure for cache [{}] with key [{}]. Cause: {}",
                        cache != null ? cache.getName() : "unknown", key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis CLEAR failure for cache [{}]. Cause: {}",
                        cache != null ? cache.getName() : "unknown", exception.getMessage());
            }
        };
    }
}
