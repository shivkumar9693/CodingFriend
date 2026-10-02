package com.aicodingmentor.common.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Production observability and metrics configuration (Day 24).
 * Registers standardized Micrometer meters for tracking submission throughput,
 * execution latencies, failure counters, AI inference tracing, and queue processing.
 */
@Configuration
public class ObservabilityMetricsConfig {

    public static final String METRIC_SUBMISSIONS_TOTAL = "app.submissions.total";
    public static final String METRIC_SUBMISSIONS_FAILED = "app.submissions.failed";
    public static final String METRIC_EXECUTION_DURATION = "app.execution.duration";
    public static final String METRIC_AI_REQUESTS_TOTAL = "app.ai.requests.total";
    public static final String METRIC_AI_REQUESTS_FAILED = "app.ai.requests.failed";
    public static final String METRIC_AI_REQUEST_DURATION = "app.ai.request.duration";
    public static final String METRIC_QUEUE_PROCESSING_DURATION = "app.queue.processing.duration";

    private final MeterRegistry registry;

    public ObservabilityMetricsConfig(MeterRegistry registry) {
        this.registry = registry;
    }

    @Bean
    public Counter submissionsTotalCounter() {
        return Counter.builder(METRIC_SUBMISSIONS_TOTAL)
                .description("Total number of code submissions received")
                .register(registry);
    }

    @Bean
    public Counter submissionsFailedCounter() {
        return Counter.builder(METRIC_SUBMISSIONS_FAILED)
                .description("Total number of code submissions that resulted in execution or compilation failures")
                .register(registry);
    }

    @Bean
    public Timer executionDurationTimer() {
        return Timer.builder(METRIC_EXECUTION_DURATION)
                .description("Wall-clock execution duration for sandboxed code runs")
                .publishPercentiles(0.5, 0.9, 0.99)
                .register(registry);
    }

    @Bean
    public Counter aiRequestsTotalCounter() {
        return Counter.builder(METRIC_AI_REQUESTS_TOTAL)
                .description("Total Socratic AI Mentor hint generation requests")
                .register(registry);
    }

    @Bean
    public Counter aiRequestsFailedCounter() {
        return Counter.builder(METRIC_AI_REQUESTS_FAILED)
                .description("Total Socratic AI Mentor inference failures/fallbacks")
                .register(registry);
    }

    @Bean
    public Timer aiRequestDurationTimer() {
        return Timer.builder(METRIC_AI_REQUEST_DURATION)
                .description("Latency of AI mentor inference requests")
                .publishPercentiles(0.5, 0.9, 0.99)
                .register(registry);
    }

    @Bean
    public Timer queueProcessingTimer() {
        return Timer.builder(METRIC_QUEUE_PROCESSING_DURATION)
                .description("Duration between submission queuing and execution completion")
                .publishPercentiles(0.5, 0.9, 0.99)
                .register(registry);
    }
}
