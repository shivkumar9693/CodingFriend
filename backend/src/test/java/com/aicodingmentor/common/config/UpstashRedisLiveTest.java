package com.aicodingmentor.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class UpstashRedisLiveTest {

    @Test
    @DisplayName("Verify live Upstash Redis connection over TLS")
    void testLiveUpstashConnection() {
        String host = "cheerful-bedbug-192396.upstash.io";
        int port = 6379;
        String password = "gQAAAAAAAu-MAAIgcDE1MThhMDllNGI4NzM0YzY0YWJjMThmNjgwNzliN2I1Mw";

        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(host, port);
        serverConfig.setPassword(password);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .useSsl()
                .and()
                .commandTimeout(Duration.ofSeconds(10))
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(serverConfig, clientConfig);
        factory.afterPropertiesSet();

        try {
            StringRedisTemplate template = new StringRedisTemplate(factory);
            template.afterPropertiesSet();

            String testKey = "codingfriend:ping:" + System.currentTimeMillis();
            template.opsForValue().set(testKey, "live_upstash_ok", Duration.ofSeconds(30));

            String retrieved = template.opsForValue().get(testKey);
            System.out.println(">>> LIVE UPSTASH REDIS VERIFIED! Retrieved value: " + retrieved);

            assertEquals("live_upstash_ok", retrieved);
            template.delete(testKey);
        } finally {
            factory.destroy();
        }
    }
}
