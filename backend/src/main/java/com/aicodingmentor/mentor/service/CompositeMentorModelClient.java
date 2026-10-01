package com.aicodingmentor.mentor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Composite AI Mentor client that seamlessly routes inference across configured LLM backends:
 * 1. Cloud AI Provider (e.g., Free Groq API or OpenRouter via {@link OpenAiCompatibleMentorModelClient}) if API key configured.
 * 2. Local Ollama LLM (via {@link OllamaMentorModelClient}) if running locally.
 * 3. Deterministic Socratic fallback if neither is available.
 */
@Component
@Primary
public class CompositeMentorModelClient implements MentorModelClient {

    private static final Logger log = LoggerFactory.getLogger(CompositeMentorModelClient.class);

    private final OpenAiCompatibleMentorModelClient openAiClient;
    private final OllamaMentorModelClient ollamaClient;

    @Autowired
    public CompositeMentorModelClient(
            OpenAiCompatibleMentorModelClient openAiClient,
            OllamaMentorModelClient ollamaClient
    ) {
        this.openAiClient = openAiClient;
        this.ollamaClient = ollamaClient;

        if (openAiClient.isConfigured()) {
            log.info("CompositeMentorModelClient active provider: Cloud AI ({})", openAiClient.getModelName());
        } else {
            log.info("CompositeMentorModelClient active provider: Local Ollama ({}) / Deterministic Fallback",
                    ollamaClient.getModelName());
        }
    }

    @Override
    public Optional<String> generate(String systemPrompt, String userPrompt) {
        // 1. If Cloud API (Groq/OpenRouter) is configured, prefer it for fast, 0-disk-overhead inference
        if (openAiClient.isConfigured()) {
            Optional<String> cloudResult = openAiClient.generate(systemPrompt, userPrompt);
            if (cloudResult.isPresent()) {
                return cloudResult;
            }
            log.warn("Cloud AI inference yielded no response. Checking local Ollama fallback...");
        }

        // 2. Try local Ollama if available
        Optional<String> ollamaResult = ollamaClient.generate(systemPrompt, userPrompt);
        if (ollamaResult.isPresent()) {
            return ollamaResult;
        }

        // 3. Fallback to deterministic rule engine
        return Optional.empty();
    }

    @Override
    public String getModelName() {
        if (openAiClient.isConfigured()) {
            return "cloud:" + openAiClient.getModelName();
        }
        return "ollama:" + ollamaClient.getModelName();
    }

    @Override
    public boolean isAvailable() {
        if (openAiClient.isConfigured()) {
            return openAiClient.isAvailable();
        }
        return ollamaClient.isAvailable();
    }

    public OpenAiCompatibleMentorModelClient getOpenAiClient() {
        return openAiClient;
    }

    public OllamaMentorModelClient getOllamaClient() {
        return ollamaClient;
    }
}
