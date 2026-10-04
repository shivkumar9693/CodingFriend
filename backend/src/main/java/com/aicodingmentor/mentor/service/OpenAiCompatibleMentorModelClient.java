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

    private record CloudProvider(String name, String baseUrl, String apiKey, List<String> models) {}

    private final List<CloudProvider> providers = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public OpenAiCompatibleMentorModelClient(
            @Value("${app.mentor.cloud.groq-api-key:${GROQ_API_KEY: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}}") String groqApiKey,
            @Value("${app.mentor.cloud.gemini-api-key:${GEMINI_API_KEY:AQ.Ab8RN6KCXZ2inXZOFrMz3nxU5tZDASISm0ZWsH8s0UU2UPboww}}") String geminiApiKey,
            @Value("${app.mentor.cloud.api-key:${AI_API_KEY:}}") String customApiKey,
            @Value("${app.mentor.cloud.base-url:${AI_BASE_URL:}}") String customBaseUrl,
            @Value("${app.mentor.cloud.model:${AI_MODEL:}}") String customModel,
            @Value("${app.mentor.cloud.timeout-seconds:${AI_TIMEOUT_SECONDS:25}}") int timeoutSeconds,
            ObjectMapper objectMapper
    ) {
        this(
                groqApiKey,
                geminiApiKey,
                customApiKey,
                customBaseUrl,
                customModel,
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
            String groqApiKey,
            String geminiApiKey,
            String customApiKey,
            String customBaseUrl,
            String customModel,
            int timeoutSeconds,
            HttpClient httpClient,
            ObjectMapper objectMapper
    ) {
        this.timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;

        // 1. Groq Cloud Provider (Current Active Production Models: Qwen first, then GPT-OSS & Llama)
        if (groqApiKey != null && !groqApiKey.isBlank()) {
            providers.add(new CloudProvider(
                    "Groq-Cloud",
                    "https://api.groq.com/openai/v1",
                    groqApiKey.trim(),
                    List.of("qwen/qwen3.8-27b", "openai/gpt-oss-120b", "openai/gpt-oss-20b", "llama-3.3-70b-versatile", "llama-3.1-8b-instant")
            ));
        }

        // 2. Custom or override provider if explicitly supplied
        if (customApiKey != null && !customApiKey.isBlank() && !customApiKey.equals(groqApiKey)) {
            String bUrl = (customBaseUrl != null && !customBaseUrl.isBlank())
                    ? customBaseUrl.replaceAll("/+$", "")
                    : (customApiKey.startsWith("AIza") || customApiKey.startsWith("AQ.")
                        ? "https://generativelanguage.googleapis.com/v1beta/openai"
                        : "https://api.groq.com/openai/v1");
            String defaultMod = (customModel != null && !customModel.isBlank())
                    ? customModel
                    : (bUrl.contains("googleapis") ? "gemini-1.5-flash" : "qwen/qwen3.8-27b");
            List<String> models = bUrl.contains("googleapis")
                    ? List.of(defaultMod, "gemini-1.5-flash", "gemini-2.0-flash", "gemini-1.5-pro")
                    : List.of(defaultMod, "qwen/qwen3.8-27b", "openai/gpt-oss-120b", "openai/gpt-oss-20b");
            providers.add(new CloudProvider("Custom-AI", bUrl, customApiKey.trim(), models));
        }

        // 3. Google Gemini Provider
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            providers.add(new CloudProvider(
                    "Google-Gemini",
                    "https://generativelanguage.googleapis.com/v1beta/openai",
                    geminiApiKey.trim(),
                    List.of("gemini-1.5-flash", "gemini-2.0-flash", "gemini-1.5-pro")
            ));
        }

        if (isConfigured()) {
            log.info("Initialized OpenAiCompatibleMentorModelClient with {} active cloud providers: {}",
                    providers.size(), providers.stream().map(p -> p.name() + " (" + maskApiKey(p.apiKey()) + ")").toList());
        } else {
            log.debug("OpenAiCompatibleMentorModelClient is not configured (no cloud API keys found).");
        }
    }

    public boolean isConfigured() {
        return !providers.isEmpty();
    }

    @Override
    public Optional<String> generate(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        // Try configured cloud providers in resilient fallback order
        for (CloudProvider provider : providers) {
            for (String modelName : provider.models()) {
                log.debug("Attempting inference with provider [{}] model [{}]...", provider.name(), modelName);
                Optional<String> res = executeInference(provider, modelName, systemPrompt, userPrompt);
                if (res.isPresent()) {
                    return res;
                }
            }
            log.warn("Provider [{}] exhausted all candidate models. Cascading to next cloud provider...", provider.name());
        }

        return Optional.empty();
    }

    private Optional<String> executeInference(CloudProvider provider, String targetModel, String systemPrompt, String userPrompt) {
        long startTime = System.currentTimeMillis();
        int promptSize = (systemPrompt != null ? systemPrompt.length() : 0) + (userPrompt != null ? userPrompt.length() : 0);

        try {
            Map<String, Object> payloadMap = Map.of(
                    "model", targetModel,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt != null ? systemPrompt : ""),
                            Map.of("role", "user", "content", userPrompt != null ? userPrompt : "")
                    ),
                    "temperature", 0.2,
                    "response_format", Map.of("type", "json_object")
            );

            String requestPayload = objectMapper.writeValueAsString(payloadMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(provider.baseUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + provider.apiKey())
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
                        log.info("Cloud AI inference succeeded via [{}] (model: [{}]) in [{}ms]",
                                provider.name(), targetModel, latencyMs);
                        return Optional.ofNullable(rawContent);
                    }
                }
                log.warn("Provider [{}] returned 200 OK but payload format was unexpected in [{}ms]", provider.name(), latencyMs);
            } else if (response.statusCode() == 429) {
                log.warn("Provider [{}] model [{}] rate limited (HTTP 429) in [{}ms]. Cascading...",
                        provider.name(), targetModel, latencyMs);
            } else {
                log.warn("Provider [{}] model [{}] returned status [{}] in [{}ms]. Error: {}",
                        provider.name(), targetModel, response.statusCode(), latencyMs, sanitizeErrorMessage(response.body()));
            }
        } catch (HttpTimeoutException ex) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.warn("Provider [{}] model [{}] timed out after [{}ms]. Cascading...",
                    provider.name(), targetModel, latencyMs);
        } catch (ConnectException ex) {
            log.warn("Provider [{}] host unreachable at [{}] ({}). Cascading...", provider.name(), provider.baseUrl(), ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Cloud AI request interrupted: {}", ex.getMessage());
        } catch (IOException ex) {
            log.warn("I/O error communicating with provider [{}] model [{}]: {}", provider.name(), targetModel, ex.getMessage());
        } catch (Exception ex) {
            log.warn("Unexpected error during Cloud AI inference with [{}] model [{}]: {}", provider.name(), targetModel, ex.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public String getModelName() {
        if (providers.isEmpty()) {
            return "unconfigured";
        }
        return providers.get(0).models().get(0);
    }

    @Override
    public boolean isAvailable() {
        if (!isConfigured()) {
            return false;
        }
        for (CloudProvider provider : providers) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(provider.baseUrl() + "/models"))
                        .header("Authorization", "Bearer " + provider.apiKey())
                        .timeout(Duration.ofSeconds(3))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return true;
                }
            } catch (Exception ex) {
                log.debug("Cloud AI availability check failed for provider [{}] : {}", provider.name(), ex.getMessage());
            }
        }
        return false;
    }

    public String getBaseUrl() {
        return providers.isEmpty() ? "" : providers.get(0).baseUrl();
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
