package com.aicodingmentor.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Automatically loads key-value pairs from .env files into Spring Boot's environment on startup.
 * Searches current directory and workspace root to ensure zero-configuration developer experience.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        File[] candidateFiles = new File[]{
                new File(".env"),
                new File("../.env"),
                new File(System.getProperty("user.dir", "."), ".env")
        };

        File resolvedEnvFile = null;
        for (File candidate : candidateFiles) {
            if (candidate.exists() && candidate.isFile()) {
                resolvedEnvFile = candidate;
                break;
            }
        }

        if (resolvedEnvFile == null) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(resolvedEnvFile.toPath());
            Map<String, Object> envMap = new HashMap<>();

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }

                int eqIdx = trimmed.indexOf('=');
                String key = trimmed.substring(0, eqIdx).trim();
                String value = trimmed.substring(eqIdx + 1).trim();

                // Strip wrapping quotes if present
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }

                if (!key.isEmpty()) {
                    envMap.put(key, value);
                }
            }

            if (!envMap.isEmpty()) {
                environment.getPropertySources().addLast(new MapPropertySource("dotenvProperties", envMap));
                log.info("Loaded [{}] environment properties from [{}]", envMap.size(), resolvedEnvFile.getAbsolutePath());
            }
        } catch (IOException ex) {
            log.warn("Failed to read .env file at [{}]: {}", resolvedEnvFile.getAbsolutePath(), ex.getMessage());
        }
    }
}
