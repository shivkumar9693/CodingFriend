package com.aicodingmentor.mentor.service;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Backward-compatible wrapper delegating to {@link OllamaMentorModelClient}.
 */
public class OllamaClient implements MentorModelClient {

    private final OllamaMentorModelClient delegate;

    public OllamaClient(OllamaMentorModelClient delegate) {
        this.delegate = delegate;
    }

    public String getModel() {
        return delegate.getModelName();
    }

    @Override
    public String getModelName() {
        return delegate.getModelName();
    }

    @Override
    public boolean isAvailable() {
        return delegate.isAvailable();
    }

    @Override
    public Optional<String> generate(String systemPrompt, String userPrompt) {
        return delegate.generate(systemPrompt, userPrompt);
    }
}
