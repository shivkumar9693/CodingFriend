package com.aicodingmentor;

import com.aicodingmentor.common.api.ApiResponse;
import com.aicodingmentor.common.error.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AiCodingMentorApplicationTests {

    @Test
    @DisplayName("Spring Application Context loads successfully")
    void contextLoads() {
        // Verifies Spring context initializes properly with test profile
    }

    @Test
    @DisplayName("ApiResponse factory methods construct valid response envelopes")
    void testApiResponseEnvelope() {
        ApiResponse<String> response = ApiResponse.success("test-data");
        assertTrue(response.success());
        assertEquals("Operation successful", response.message());
        assertEquals("test-data", response.data());
        assertNotNull(response.timestamp());
    }

    @Test
    @DisplayName("ErrorResponse factory methods construct valid error envelopes with validation errors")
    void testErrorResponseEnvelope() {
        ErrorResponse.ValidationError validationError = new ErrorResponse.ValidationError("sourceCode", "must not be blank");
        ErrorResponse errorResponse = ErrorResponse.of(400, "Bad Request", "Validation failed", List.of(validationError));

        assertFalse(errorResponse.success());
        assertEquals(400, errorResponse.status());
        assertEquals("Bad Request", errorResponse.error());
        assertEquals(1, errorResponse.validationErrors().size());
        assertEquals("sourceCode", errorResponse.validationErrors().get(0).field());
    }
}
