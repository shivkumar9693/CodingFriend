package com.aicodingmentor.mentor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Ollama adapter implementing {@link MentorModelClient} for local LLM inference.
 * Supports configurable base URL, model name, and execution timeouts via environment variables.
 * Fails gracefully when Ollama is unreachable without exposing learner source code in error logs.
 */
@Component
public class OllamaMentorModelClient implements MentorModelClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaMentorModelClient.class);

    private final String baseUrl;
    private final String model;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public OllamaMentorModelClient(
            @Value("${app.mentor.ollama.base-url:${OLLAMA_BASE_URL:http://localhost:11434}}") String baseUrl,
            @Value("${app.mentor.ollama.model:${OLLAMA_MODEL:qwen2.5-coder:latest}}") String model,
            @Value("${app.mentor.ollama.timeout-seconds:${OLLAMA_TIMEOUT_SECONDS:60}}") int timeoutSeconds,
            ObjectMapper objectMapper
    ) {
        this(
                baseUrl,
                model,
                timeoutSeconds,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build(),
                objectMapper
        );
    }

    /**
     * Package-private constructor for unit testing with mocked HTTP clients.
     */
    OllamaMentorModelClient(
            String baseUrl,
            String model,
            int timeoutSeconds,
            HttpClient httpClient,
            ObjectMapper objectMapper
    ) {
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.replaceAll("/+$", "") : "http://localhost:11434";
        this.model = (model != null && !model.isBlank()) ? model : "qwen2.5-coder:latest";
        this.timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        log.info("Initialized OllamaMentorModelClient with target [{}], model [{}], timeout [{}s]",
                this.baseUrl, this.model, this.timeout.toSeconds());
    }

    @Override
    public Optional<String> generate(String systemPrompt, String userPrompt) {
        long startTime = System.currentTimeMillis();
        int promptSize = (systemPrompt != null ? systemPrompt.length() : 0) + (userPrompt != null ? userPrompt.length() : 0);

        log.debug("Dispatching LLM inference request to Ollama. Model: [{}], Total Prompt Chars: [{}]",
                model, promptSize);

        try {
            String requestPayload = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "system", systemPrompt != null ? systemPrompt : "",
                    "prompt", userPrompt != null ? userPrompt : "",
                    "stream", false,
                    "format", "json"
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .timeout(timeout)
                    .POST(HttpRequest.BodyPublishers.ofString(requestPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latencyMs = System.currentTimeMillis() - startTime;

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.has("response")) {
                    String rawJson = root.get("response").asText();
                    log.info("Ollama inference completed successfully in [{}ms] for model [{}]", latencyMs, model);
                    return Optional.ofNullable(rawJson);
                } else {
                    log.warn("Ollama returned 200 OK but payload missing 'response' node in [{}ms]", latencyMs);
                }
            } else {
                log.warn("Ollama server returned non-200 status [{}] in [{}ms]. Error: {}",
                        response.statusCode(), latencyMs, sanitizeErrorMessage(response.body()));
            }
        } catch (HttpTimeoutException ex) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.warn("Ollama inference timed out after [{}ms] (limit: {}s). Engaging Socratic rule fallback.",
                    latencyMs, timeout.toSeconds());
        } catch (ConnectException ex) {
            log.debug("Ollama is not reachable at [{}] ({}). Engaging Socratic rule fallback.", baseUrl, ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Ollama request interrupted: {}", ex.getMessage());
        } catch (IOException ex) {
            log.warn("I/O error communicating with Ollama at [{}]: {}", baseUrl, ex.getMessage());
        } catch (Exception ex) {
            log.warn("Unexpected error during Ollama inference: {}", ex.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception ex) {
            log.debug("Ollama availability check failed against [{}] : {}", baseUrl, ex.getMessage());
            return false;
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Duration getTimeout() {
        return timeout;
    }

    private String sanitizeErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "<empty response>";
        }
        // Truncate long error bodies to prevent log flooding
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }
}
