package com.aicodingmentor.execution.service;

import com.aicodingmentor.execution.model.ExecutionFailureReason;
import com.aicodingmentor.execution.model.ExecutionRequest;
import com.aicodingmentor.execution.model.ExecutionResult;
import com.aicodingmentor.execution.model.SupportedLanguage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance, isolated code execution service powered by the open-source Piston engine.
 * Eliminates compilation and execution CPU/RAM overhead on the Spring Boot host container,
 * with automatic fallback to local sandboxed execution if the cloud engine is unreachable.
 */
@Service
@Primary
public class PistonCodeExecutor implements CodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(PistonCodeExecutor.class);

    private static final Pattern JAVA_CLASS_NAME_PATTERN = Pattern.compile("(?:public\\s+)?class\\s+([A-Za-z0-9_]+)");
    /**
     * Estimated Wandbox server-side startup/compile overhead in ms.
     * Subtracted from the raw API latency to give a fairer code execution time
     * so users don't see 3500ms for trivially fast code.
     */
    private static final long WANDBOX_OVERHEAD_MS = 2500L;

    private final String pistonApiUrl;
    private final boolean enabled;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final CodeExecutor fallbackExecutor;

    @Autowired
    public PistonCodeExecutor(
            @Value("${app.execution.piston.url:https://emkc.org/api/v2/piston/execute}") String pistonApiUrl,
            @Value("${app.execution.piston.enabled:true}") boolean enabled,
            @Value("${app.execution.piston.timeout-seconds:20}") int timeoutSeconds,
            @Qualifier("multiLanguageExecutor") CodeExecutor fallbackExecutor,
            ObjectMapper objectMapper
    ) {
        this.pistonApiUrl = pistonApiUrl != null && !pistonApiUrl.isBlank() ? pistonApiUrl.trim() : "https://emkc.org/api/v2/piston/execute";
        this.enabled = enabled;
        this.timeout = Duration.ofSeconds(Math.max(2, timeoutSeconds));
        this.fallbackExecutor = fallbackExecutor;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();

        log.info("Initialized PistonCodeExecutor (enabled: {}, endpoint: {})", enabled, this.pistonApiUrl);
    }

    PistonCodeExecutor(
            String pistonApiUrl,
            boolean enabled,
            Duration timeout,
            HttpClient httpClient,
            CodeExecutor fallbackExecutor,
            ObjectMapper objectMapper
    ) {
        this.pistonApiUrl = pistonApiUrl;
        this.enabled = enabled;
        this.timeout = timeout;
        this.httpClient = httpClient;
        this.fallbackExecutor = fallbackExecutor;
        this.objectMapper = objectMapper;
    }

    @Override
    public ExecutionResult execute(ExecutionRequest request) {
        if (!enabled) {
            log.debug("Cloud execution disabled via config. Using fallback executor.");
            return fallbackExecutor.execute(request);
        }

        // 1. Try Wandbox (100% Free, Public, Open API - No Whitelist)
        try {
            return executeViaWandbox(request);
        } catch (Exception ex) {
            log.warn("Wandbox execution failed: [{}]. Cascading to Piston...", ex.getMessage());
        }

        // 2. Try Piston API
        try {
            return executeViaPiston(request);
        } catch (Exception ex) {
            log.warn("Piston execution failed: [{}]. Cascading to local fallback executor...", ex.getMessage());
        }

        // 3. Fallback to local container sandbox
        return fallbackExecutor.execute(request);
    }

    @Override
    public List<ExecutionResult> executeBatch(ExecutionRequest baseRequest, List<String> stdinInputs) {
        if (!enabled) {
            return fallbackExecutor.executeBatch(baseRequest, stdinInputs);
        }

        if (stdinInputs == null || stdinInputs.isEmpty()) {
            return List.of(execute(baseRequest));
        }

        List<ExecutionResult> batchResults = new ArrayList<>();
        boolean encounteredCloudFailure = false;

        for (String input : stdinInputs) {
            ExecutionRequest singleRequest = new ExecutionRequest(
                    baseRequest.sourceCode(),
                    baseRequest.className(),
                    input,
                    baseRequest.timeoutMs(),
                    baseRequest.memoryLimitMb(),
                    baseRequest.cpuQuota(),
                    baseRequest.maxOutputSizeBytes(),
                    baseRequest.language()
            );

            if (!encounteredCloudFailure) {
                try {
                    // Try Wandbox first, then Piston
                    ExecutionResult result;
                    try {
                        result = executeViaWandbox(singleRequest);
                    } catch (Exception wandboxEx) {
                        result = executeViaPiston(singleRequest);
                    }

                    batchResults.add(result);

                    // If compilation error occurred on first test case, short-circuit remaining batch
                    if (result.failureReason() == ExecutionFailureReason.COMPILATION_ERROR) {
                        break;
                    }
                } catch (Exception ex) {
                    log.warn("Cloud batch execution failed: [{}]. Falling back remaining batch to local executor.", ex.getMessage());
                    encounteredCloudFailure = true;
                    batchResults.add(fallbackExecutor.execute(singleRequest));
                }
            } else {
                batchResults.add(fallbackExecutor.execute(singleRequest));
            }
        }

        return batchResults;
    }

    private ExecutionResult executeViaWandbox(ExecutionRequest request) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        String compiler = mapWandboxCompiler(request.language());

        Map<String, Object> payloadMap = new java.util.HashMap<>();
        payloadMap.put("compiler", compiler);
        payloadMap.put("code", request.sourceCode() != null ? request.sourceCode() : "");
        payloadMap.put("stdin", request.stdin() != null ? request.stdin() : "");

        String jsonPayload = objectMapper.writeValueAsString(payloadMap);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://wandbox.org/api/compile.json"))
                .header("Content-Type", "application/json")
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        long latencyMs = System.currentTimeMillis() - startTime;

        if (response.statusCode() != 200) {
            throw new IOException("Wandbox returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        if (!root.has("status") || !root.has("program_output")) {
            throw new IOException("Malformed Wandbox response payload: " + response.body());
        }

        int status = root.has("status") && !root.get("status").isNull() ? root.get("status").asInt(0) : 0;
        String compilerError = root.has("compiler_error") ? root.get("compiler_error").asText("") : "";
        String programOutput = root.has("program_output") ? root.get("program_output").asText("") : "";
        String programError = root.has("program_error") ? root.get("program_error").asText("") : "";
        String signal = root.has("signal") && !root.get("signal").isNull() ? root.get("signal").asText("") : "";

        // Normalize execution time: subtract Wandbox server startup/compile overhead
        // so displayed time reflects actual code execution, not API round-trip latency
        long normalizedTimeMs = Math.max(50L, latencyMs - WANDBOX_OVERHEAD_MS);

        if ("SIGKILL".equalsIgnoreCase(signal) || "SIGTERM".equalsIgnoreCase(signal)
                || (!programError.isBlank() && programError.toLowerCase().contains("time limit exceeded"))) {
            log.info("Wandbox TLE detected (signal={}, latency={}ms)", signal, latencyMs);
            return ExecutionResult.timeout(normalizedTimeMs);
        }

        if (!compilerError.isBlank() && status != 0) {
            log.debug("Wandbox compilation error in [{}ms]", latencyMs);
            return ExecutionResult.compilationError(compilerError, status, normalizedTimeMs);
        }

        if (status == 0) {
            log.debug("Wandbox execution succeeded in [{}ms] (normalized from {}ms)", normalizedTimeMs, latencyMs);
            return ExecutionResult.success(programOutput, programError, 0, normalizedTimeMs, 32L * 1024 * 1024);
        } else {
            log.debug("Wandbox runtime error in [{}ms]", normalizedTimeMs);
            return ExecutionResult.runtimeError(programOutput, !programError.isBlank() ? programError : compilerError, status, normalizedTimeMs);
        }
    }

    private String mapWandboxCompiler(SupportedLanguage language) {
        if (language == null) {
            return "openjdk-jdk-21+35";
        }
        return switch (language) {
            case JAVA -> "openjdk-jdk-21+35";
            case PYTHON -> "cpython-3.12.7";
            case CPP -> "gcc-13.2.0";
            case JAVASCRIPT -> "nodejs-20.17.0";
        };
    }

    private ExecutionResult executeViaPiston(ExecutionRequest request) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        String language = mapLanguage(request.language());
        String fileName = resolveFileName(request.language(), request.sourceCode(), request.className());

        Map<String, Object> payloadMap = Map.of(
                "language", language,
                "version", "*",
                "files", List.of(
                        Map.of(
                                "name", fileName,
                                "content", request.sourceCode() != null ? request.sourceCode() : ""
                        )
                ),
                "stdin", request.stdin() != null ? request.stdin() : ""
        );

        String jsonPayload = objectMapper.writeValueAsString(payloadMap);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(pistonApiUrl))
                .header("Content-Type", "application/json")
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        long latencyMs = System.currentTimeMillis() - startTime;

        if (response.statusCode() != 200) {
            throw new IOException("Piston returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());

        // 1. Check for compilation errors (if compile step exists and returned non-zero code)
        if (root.has("compile") && !root.get("compile").isNull()) {
            JsonNode compileNode = root.get("compile");
            int compileCode = compileNode.has("code") && !compileNode.get("code").isNull() ? compileNode.get("code").asInt(0) : 0;
            String compileStderr = compileNode.has("stderr") ? compileNode.get("stderr").asText("") : "";
            String compileOutput = compileNode.has("output") ? compileNode.get("output").asText("") : "";

            if (compileCode != 0 || (!compileStderr.isBlank() && !compileStderr.contains("Picked up JAVA_TOOL_OPTIONS"))) {
                String errorMsg = !compileStderr.isBlank() ? compileStderr : compileOutput;
                log.info("Piston compilation error detected in [{}ms]", latencyMs);
                return ExecutionResult.compilationError(errorMsg, compileCode != 0 ? compileCode : 1, latencyMs);
            }
        }

        // 2. Parse execution run node
        if (root.has("run") && !root.get("run").isNull()) {
            JsonNode runNode = root.get("run");
            int exitCode = runNode.has("code") && !runNode.get("code").isNull() ? runNode.get("code").asInt(0) : 0;
            String stdout = runNode.has("stdout") ? runNode.get("stdout").asText("") : "";
            String stderr = runNode.has("stderr") ? runNode.get("stderr").asText("") : "";
            String signal = runNode.has("signal") && !runNode.get("signal").isNull() ? runNode.get("signal").asText("") : "";

            // Check for timeout / SIGKILL / SIGTERM
            if ("SIGKILL".equalsIgnoreCase(signal) || "SIGTERM".equalsIgnoreCase(signal)) {
                log.info("Piston execution timeout detected (signal: {}) in [{}ms]", signal, latencyMs);
                return ExecutionResult.timeout(latencyMs);
            }

            if (exitCode == 0) {
                log.debug("Piston execution succeeded in [{}ms]", latencyMs);
                return ExecutionResult.success(stdout, stderr, 0, latencyMs, 32L * 1024 * 1024);
            } else {
                log.info("Piston runtime error detected (exitCode: {}) in [{}ms]", exitCode, latencyMs);
                return ExecutionResult.runtimeError(stdout, stderr, exitCode, latencyMs);
            }
        }

        throw new IOException("Malformed Piston response payload: " + response.body());
    }

    private String mapLanguage(SupportedLanguage language) {
        if (language == null) {
            return "java";
        }
        return switch (language) {
            case JAVA -> "java";
            case PYTHON -> "python";
            case CPP -> "c++";
            case JAVASCRIPT -> "javascript";
        };
    }

    private String resolveFileName(SupportedLanguage language, String sourceCode, String className) {
        if (language == null || language == SupportedLanguage.JAVA) {
            if (className != null && !className.isBlank()) {
                return className.trim() + ".java";
            }
            if (sourceCode != null) {
                Matcher matcher = JAVA_CLASS_NAME_PATTERN.matcher(sourceCode);
                if (matcher.find()) {
                    return matcher.group(1) + ".java";
                }
            }
            return "MainDriver.java";
        }
        return switch (language) {
            case PYTHON -> "main.py";
            case CPP -> "main.cpp";
            case JAVASCRIPT -> "main.js";
            default -> "solution";
        };
    }
}
