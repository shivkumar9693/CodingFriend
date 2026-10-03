package com.aicodingmentor.common.config;

import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.problem.dto.ProblemDetailResponse;
import com.aicodingmentor.problem.dto.TestCaseResponse;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RedisCacheSerializationTest {

    @Test
    @DisplayName("Should serialize and deserialize ProblemDetailResponse Record successfully")
    void testRecordSerializationAndDeserialization() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

        TestCaseResponse tc = new TestCaseResponse(UUID.randomUUID(), "[2, 7]\n9", "[0, 1]", false, 1, "Sample", null);
        ProblemDetailResponse response = new ProblemDetailResponse(
                UUID.randomUUID(),
                "Two Sum",
                "two-sum",
                "Description",
                ProblemDifficulty.EASY,
                "Constraints",
                "Input",
                "Output",
                "Examples",
                "Starter",
                "JAVA",
                List.of("JAVA", "PYTHON"),
                Map.of("JAVA", "code"),
                Set.of("ARRAY"),
                List.of(tc),
                Instant.now(),
                Instant.now()
        );

        byte[] serialized = serializer.serialize(response);
        assertNotNull(serialized);
        assertTrue(serialized.length > 0);

        Object deserialized = serializer.deserialize(serialized);
        assertNotNull(deserialized);
        assertInstanceOf(ProblemDetailResponse.class, deserialized);

        ProblemDetailResponse result = (ProblemDetailResponse) deserialized;
        assertEquals("Two Sum", result.title());
        assertEquals("two-sum", result.slug());
        assertEquals(1, result.sampleTestCases().size());
    }
}
