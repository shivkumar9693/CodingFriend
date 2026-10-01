package com.aicodingmentor.mentor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
@DisplayName("OpenAiCompatibleMentorModelClient Unit Tests")
class OpenAiCompatibleMentorModelClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private ObjectMapper objectMapper;
    private OpenAiCompatibleMentorModelClient client;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        client = new OpenAiCompatibleMentorModelClient(
                "gsk_testApiKey123456789",
                "https://api.groq.com/openai/v1",
                "llama-3.3-70b-versatile",
                10,
                httpClient,
                objectMapper
        );
    }

    @Test
    @DisplayName("Should detect configured status when API key is provided")
    void testIsConfigured() {
        assertThat(client.isConfigured()).isTrue();

        OpenAiCompatibleMentorModelClient unconfigured = new OpenAiCompatibleMentorModelClient(
                "",
                "https://api.groq.com/openai/v1",
                "llama-3.3-70b-versatile",
                10,
                httpClient,
                objectMapper
        );
        assertThat(unconfigured.isConfigured()).isFalse();
        assertThat(unconfigured.generate("system", "user")).isEmpty();
    }

    @Test
    @DisplayName("Should parse OpenAI-compatible JSON response on 200 OK")
    void testGenerate_success() throws Exception {
        String jsonPayload = """
                {
                  "id": "chatcmpl-123",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "{\\"title\\":\\"Socratic Guidance\\",\\"guidance\\":\\"Check your loop boundary.\\"}"
                      },
                      "finish_reason": "stop"
                    }
                  ]
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonPayload);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        Optional<String> result = client.generate("You are a Socratic mentor.", "My loop goes out of bounds.");

        assertThat(result).isPresent();
        assertThat(result.get()).contains("Check your loop boundary.");
        assertThat(client.getModelName()).isEqualTo("llama-3.3-70b-versatile");
    }

    @Test
    @DisplayName("Should handle 429 rate limits gracefully by returning empty Optional")
    void testGenerate_rateLimit429() throws Exception {
        when(httpResponse.statusCode()).thenReturn(429);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        Optional<String> result = client.generate("system", "user");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle 500 error gracefully by returning empty Optional")
    void testGenerate_non200Error() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("Internal Server Error");
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        Optional<String> result = client.generate("system", "user");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle timeout gracefully")
    void testGenerate_timeout() throws Exception {
        doThrow(new HttpTimeoutException("Cloud request timed out"))
                .when(httpClient).send(any(HttpRequest.class), any());

        Optional<String> result = client.generate("system", "user");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle connection refusal gracefully")
    void testGenerate_connectionRefused() throws Exception {
        doThrow(new ConnectException("Host unreachable"))
                .when(httpClient).send(any(HttpRequest.class), any());

        Optional<String> result = client.generate("system", "user");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should report isAvailable true when /models returns 200")
    void testIsAvailable_true() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        assertThat(client.isAvailable()).isTrue();
    }
}
