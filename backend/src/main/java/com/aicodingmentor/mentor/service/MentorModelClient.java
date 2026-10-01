package com.aicodingmentor.mentor.service;

import java.util.Optional;

/**
 * Pluggable abstraction for AI Mentor model clients.
 * Decouples the Socratic guidance engine from concrete LLM providers and local inference backends.
 */
public interface MentorModelClient {

    /**
     * Generates a response from the mentor LLM with structured formatting.
     *
     * @param systemPrompt the system-level role instruction and Socratic constraints
     * @param userPrompt the problem context, learner code, and hint tier request
     * @return Optional containing the raw JSON response string, or Optional.empty() if unavailable or on error
     */
    Optional<String> generate(String systemPrompt, String userPrompt);

    /**
     * Returns the name or identifier of the configured model.
     *
     * @return model name string (e.g., "qwen2.5-coder:latest", "deepseek-coder:6.7b")
     */
    String getModelName();

    /**
     * Checks if the underlying model service is currently reachable and responsive.
     *
     * @return true if the model backend is online, false otherwise
     */
    boolean isAvailable();
}
