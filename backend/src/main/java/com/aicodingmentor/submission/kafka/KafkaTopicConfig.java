package com.aicodingmentor.submission.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares Kafka topics for the asynchronous submission, execution, and learning pipeline.
 */
@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaTopicConfig {

    public static final String TOPIC_CODE_SUBMISSIONS = "code-submissions";
    public static final String TOPIC_CODE_SUBMISSIONS_DLT = "code-submissions-dlt";
    public static final String TOPIC_EXECUTION_RESULTS = "execution-results";
    public static final String TOPIC_AI_ANALYSIS = "ai-analysis";
    public static final String TOPIC_LEARNING_EVENTS = "learning-events";

    @Bean
    public NewTopic codeSubmissionsTopic() {
        return TopicBuilder.name(TOPIC_CODE_SUBMISSIONS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic codeSubmissionsDltTopic() {
        return TopicBuilder.name(TOPIC_CODE_SUBMISSIONS_DLT)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic executionResultsTopic() {
        return TopicBuilder.name(TOPIC_EXECUTION_RESULTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic aiAnalysisTopic() {
        return TopicBuilder.name(TOPIC_AI_ANALYSIS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic learningEventsTopic() {
        return TopicBuilder.name(TOPIC_LEARNING_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
