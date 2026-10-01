package com.aicodingmentor.mentor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OllamaMentorModelClient Unit Tests")
class OllamaMentorModelClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private ObjectMapper objectMapper;
    private OllamaMentorModelClient client;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        client = new OllamaMentorModelClient(
                "http://localhost:11434",
                "qwen2.5-coder:7b",
                5,
                httpClient,
                objectMapper
        );
    }

    @Test
    @DisplayName("Should successfully parse Ollama JSON response on 200 OK")
    void testGenerate_success() throws Exception {
        String ollamaJsonPayload = """
                {
                  "model": "qwen2.5-coder:7b",
                  "created_at": "2026-10-01T10:00:00Z",
                  "response": "{\\"title\\":\\"Socratic Guidance\\",\\"guidance\\":\\"Consider the hash map approach.\\"}",
                  "done": true
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(ollamaJsonPayload);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        Optional<String> result = client.generate("You are a Socratic Mentor.", "Help me with Two Sum.");

        assertThat(result).isPresent();
        assertThat(result.get()).contains("Consider the hash map approach.");
        assertThat(client.getModelName()).isEqualTo("qwen2.5-coder:7b");
    }

    @Test
    @DisplayName("Should return empty Optional when Ollama returns non-200 status")
    void testGenerate_non200Status() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("Internal Server Error");
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        Optional<String> result = client.generate("System", "Prompt");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty Optional gracefully on connection refusal (Ollama offline)")
    void testGenerate_connectionRefused() throws Exception {
        doThrow(new ConnectException("Connection refused to http://localhost:11434"))
                .when(httpClient).send(any(HttpRequest.class), any());

        Optional<String> result = client.generate("System", "Prompt");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty Optional gracefully on HTTP request timeout")
    void testGenerate_timeout() throws Exception {
        doThrow(new HttpTimeoutException("Request timed out"))
                .when(httpClient).send(any(HttpRequest.class), any());

        Optional<String> result = client.generate("System", "Prompt");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should report available when /api/tags returns 200")
    void testIsAvailable_true() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        assertThat(client.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("Should report unavailable when /api/tags fails or throws")
    void testIsAvailable_false() throws Exception {
        doThrow(new ConnectException("Connection refused"))
                .when(httpClient).send(any(HttpRequest.class), any());

        assertThat(client.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("Should strip trailing slashes from base URL properly")
    void testBaseUrlSanitization() {
        OllamaMentorModelClient c = new OllamaMentorModelClient(
                "http://localhost:11434///",
                "llama3:8b",
                30,
                httpClient,
                objectMapper
        );
        assertThat(c.getBaseUrl()).isEqualTo("http://localhost:11434");
        assertThat(c.getModelName()).isEqualTo("llama3:8b");
        assertThat(c.getTimeout().toSeconds()).isEqualTo(30);
    }
}
