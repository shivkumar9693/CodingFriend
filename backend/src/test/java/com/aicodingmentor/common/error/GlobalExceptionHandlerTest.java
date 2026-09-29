package com.aicodingmentor.common.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleApiException produces correct HTTP status and ErrorResponse")
    void testHandleApiException() {
        ApiException ex = new ResourceNotFoundException("Problem", "two-sum");
        ResponseEntity<ErrorResponse> response = handler.handleApiException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
        assertTrue(response.getBody().message().contains("Problem not found with id/slug: 'two-sum'"));
    }

    @Test
    @DisplayName("handleGeneralException produces 500 Internal Server Error")
    void testHandleGeneralException() {
        RuntimeException ex = new RuntimeException("Unexpected error");
        ResponseEntity<ErrorResponse> response = handler.handleGeneralException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals(500, response.getBody().status());
        assertEquals("Internal Server Error", response.getBody().error());
        assertEquals("Unexpected error", response.getBody().message());
    }
}
