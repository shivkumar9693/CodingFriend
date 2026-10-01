package com.aicodingmentor.mentor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OpenAI-compatible cloud client implementing {@link MentorModelClient}.
 * Connects to high-speed, permanently free LLM providers like Groq Cloud (or OpenRouter)
 * without requiring heavy local disk installations or paid subscriptions.
 */
@Component
public class OpenAiCompatibleMentorModelClient implements MentorModelClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleMentorModelClient.class);

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public OpenAiCompatibleMentorModelClient(
            @Value("${app.mentor.cloud.api-key:${GROQ_API_KEY:${AI_API_KEY:}}}") String apiKey,
            @Value("${app.mentor.cloud.base-url:${GROQ_BASE_URL:${AI_BASE_URL:https://api.groq.com/openai/v1}}}") String baseUrl,
            @Value("${app.mentor.cloud.model:${GROQ_MODEL:${AI_MODEL:llama-3.3-70b-versatile}}}") String model,
            @Value("${app.mentor.cloud.timeout-seconds:${AI_TIMEOUT_SECONDS:30}}") int timeoutSeconds,
            ObjectMapper objectMapper
    ) {
        this(
                apiKey,
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
    OpenAiCompatibleMentorModelClient(
            String apiKey,
            String baseUrl,
            String model,
            int timeoutSeconds,
            HttpClient httpClient,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.replaceAll("/+$", "") : "https://api.groq.com/openai/v1";
        this.model = (model != null && !model.isBlank()) ? model : "llama-3.3-70b-versatile";
        this.timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;

        if (isConfigured()) {
            log.info("Initialized OpenAiCompatibleMentorModelClient for provider [{}], model [{}], key [{}]",
                    this.baseUrl, this.model, maskApiKey(this.apiKey));
        } else {
            log.debug("OpenAiCompatibleMentorModelClient is not configured (no API key provided).");
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public Optional<String> generate(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        long startTime = System.currentTimeMillis();
        int promptSize = (systemPrompt != null ? systemPrompt.length() : 0) + (userPrompt != null ? userPrompt.length() : 0);

        log.debug("Dispatching LLM inference request to Cloud AI API [{}]. Model: [{}], Total Prompt Chars: [{}]",
                baseUrl, model, promptSize);

        try {
            Map<String, Object> payloadMap = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt != null ? systemPrompt : ""),
                            Map.of("role", "user", "content", userPrompt != null ? userPrompt : "")
                    ),
                    "temperature", 0.2,
                    "response_format", Map.of("type", "json_object")
            );

            String requestPayload = objectMapper.writeValueAsString(payloadMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(timeout)
                    .POST(HttpRequest.BodyPublishers.ofString(requestPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latencyMs = System.currentTimeMillis() - startTime;

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.has("choices") && root.get("choices").isArray() && !root.get("choices").isEmpty()) {
                    JsonNode messageNode = root.get("choices").get(0).get("message");
                    if (messageNode != null && messageNode.has("content")) {
                        String rawContent = messageNode.get("content").asText();
                        log.info("Cloud AI inference completed successfully in [{}ms] for model [{}]", latencyMs, model);
                        return Optional.ofNullable(rawContent);
                    }
                }
                log.warn("Cloud AI returned 200 OK but payload format unexpected in [{}ms]", latencyMs);
            } else if (response.statusCode() == 429) {
                log.warn("Cloud AI rate limit reached (HTTP 429) in [{}ms]. Engaging fallback.", latencyMs);
            } else {
                log.warn("Cloud AI server returned non-200 status [{}] in [{}ms]. Error: {}",
                        response.statusCode(), latencyMs, sanitizeErrorMessage(response.body()));
            }
        } catch (HttpTimeoutException ex) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.warn("Cloud AI inference timed out after [{}ms] (limit: {}s). Engaging fallback.",
                    latencyMs, timeout.toSeconds());
        } catch (ConnectException ex) {
            log.warn("Cloud AI host unreachable at [{}] ({}). Engaging fallback.", baseUrl, ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Cloud AI request interrupted: {}", ex.getMessage());
        } catch (IOException ex) {
            log.warn("I/O error communicating with Cloud AI API [{}]: {}", baseUrl, ex.getMessage());
        } catch (Exception ex) {
            log.warn("Unexpected error during Cloud AI inference: {}", ex.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public boolean isAvailable() {
        if (!isConfigured()) {
            return false;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/models"))
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception ex) {
            log.debug("Cloud AI availability check failed against [{}] : {}", baseUrl, ex.getMessage());
            return false;
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Duration getTimeout() {
        return timeout;
    }

    private String maskApiKey(String key) {
        if (key == null || key.length() <= 8) {
            return "******";
        }
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }

    private String sanitizeErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "<empty response>";
        }
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }
}
