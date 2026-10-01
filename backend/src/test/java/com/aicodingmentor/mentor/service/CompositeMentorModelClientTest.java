package com.aicodingmentor.mentor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompositeMentorModelClient Unit Tests")
class CompositeMentorModelClientTest {

    @Mock
    private OpenAiCompatibleMentorModelClient openAiClient;

    @Mock
    private OllamaMentorModelClient ollamaClient;

    private CompositeMentorModelClient compositeClient;

    @BeforeEach
    void setUp() {
        compositeClient = new CompositeMentorModelClient(openAiClient, ollamaClient);
    }

    @Test
    @DisplayName("Should route to Cloud AI first when configured")
    void testRoutesToCloudFirst() {
        when(openAiClient.isConfigured()).thenReturn(true);
        when(openAiClient.generate("sys", "user")).thenReturn(Optional.of("{\"guidance\":\"cloud hint\"}"));

        Optional<String> result = compositeClient.generate("sys", "user");

        assertThat(result).isPresent();
        assertThat(result.get()).contains("cloud hint");
        verify(openAiClient, times(1)).generate("sys", "user");
        verify(ollamaClient, never()).generate(anyString(), anyString());
    }

    @Test
    @DisplayName("Should fallback to Ollama when Cloud AI is configured but returns empty")
    void testCloudFailsFallsBackToOllama() {
        when(openAiClient.isConfigured()).thenReturn(true);
        when(openAiClient.generate("sys", "user")).thenReturn(Optional.empty());
        when(ollamaClient.generate("sys", "user")).thenReturn(Optional.of("{\"guidance\":\"ollama hint\"}"));

        Optional<String> result = compositeClient.generate("sys", "user");

        assertThat(result).isPresent();
        assertThat(result.get()).contains("ollama hint");
        verify(openAiClient, times(1)).generate("sys", "user");
        verify(ollamaClient, times(1)).generate("sys", "user");
    }

    @Test
    @DisplayName("Should route directly to Ollama when Cloud AI is not configured")
    void testRoutesToOllamaWhenCloudNotConfigured() {
        when(openAiClient.isConfigured()).thenReturn(false);
        when(ollamaClient.generate("sys", "user")).thenReturn(Optional.of("{\"guidance\":\"ollama only\"}"));

        Optional<String> result = compositeClient.generate("sys", "user");

        assertThat(result).isPresent();
        assertThat(result.get()).contains("ollama only");
        verify(openAiClient, never()).generate(anyString(), anyString());
        verify(ollamaClient, times(1)).generate("sys", "user");
    }

    @Test
    @DisplayName("Should return empty when all LLM providers fail or are unavailable")
    void testAllProvidersFailReturnsEmpty() {
        when(openAiClient.isConfigured()).thenReturn(false);
        when(ollamaClient.generate("sys", "user")).thenReturn(Optional.empty());

        Optional<String> result = compositeClient.generate("sys", "user");

        assertThat(result).isEmpty();
    }
}
