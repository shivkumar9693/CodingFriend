package com.aicodingmentor.submission.kafka;

import com.aicodingmentor.submission.event.AiAnalysisEvent;
import com.aicodingmentor.submission.event.CodeSubmissionEvent;
import com.aicodingmentor.submission.event.ExecutionResultEvent;
import com.aicodingmentor.submission.event.LearningEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.aicodingmentor.submission.kafka.KafkaTopicConfig.*;

/**
 * Kafka implementation of SubmissionEventProducer.
 * Publishes structured JSON events with submission UUID partition keys.
 */
@Service
public class KafkaSubmissionEventProducer implements SubmissionEventProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaSubmissionEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final boolean kafkaEnabled;

    public KafkaSubmissionEventProducer(
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.enabled:false}") boolean kafkaEnabled
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaEnabled = kafkaEnabled;
    }

    @Override
    public void sendCodeSubmission(CodeSubmissionEvent event) {
        if (!kafkaEnabled || kafkaTemplate == null) {
            log.debug("Kafka disabled or unavailable; skipping message dispatch to [{}] for submission [{}]",
                    TOPIC_CODE_SUBMISSIONS, event.submissionId());
            return;
        }

        try {
            String key = event.submissionId().toString();
            kafkaTemplate.send(TOPIC_CODE_SUBMISSIONS, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish CodeSubmissionEvent for submission [{}]", event.submissionId(), ex);
                        } else {
                            log.info("Published CodeSubmissionEvent for submission [{}] to partition [{}] at offset [{}]",
                                    event.submissionId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception ex) {
            log.error("Unexpected error publishing CodeSubmissionEvent for submission [{}]", event.submissionId(), ex);
        }
    }

    @Override
    public void sendExecutionResult(ExecutionResultEvent event) {
        if (!kafkaEnabled || kafkaTemplate == null) {
            log.debug("Kafka disabled or unavailable; skipping message dispatch to [{}] for submission [{}]",
                    TOPIC_EXECUTION_RESULTS, event.submissionId());
            return;
        }

        try {
            String key = event.submissionId().toString();
            kafkaTemplate.send(TOPIC_EXECUTION_RESULTS, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish ExecutionResultEvent for submission [{}]", event.submissionId(), ex);
                        } else {
                            log.info("Published ExecutionResultEvent for submission [{}] with verdict [{}]",
                                    event.submissionId(), event.verdict());
                        }
                    });
        } catch (Exception ex) {
            log.error("Unexpected error publishing ExecutionResultEvent for submission [{}]", event.submissionId(), ex);
        }
    }

    @Override
    public void sendAiAnalysis(AiAnalysisEvent event) {
        if (!kafkaEnabled || kafkaTemplate == null) {
            log.debug("Kafka disabled or unavailable; skipping message dispatch to [{}] for submission [{}]",
                    TOPIC_AI_ANALYSIS, event.submissionId());
            return;
        }

        try {
            String key = event.submissionId().toString();
            kafkaTemplate.send(TOPIC_AI_ANALYSIS, key, event);
            log.info("Published AiAnalysisEvent for submission [{}] with mistake type [{}]",
                    event.submissionId(), event.mistakeType());
        } catch (Exception ex) {
            log.error("Unexpected error publishing AiAnalysisEvent for submission [{}]", event.submissionId(), ex);
        }
    }

    @Override
    public void sendLearningEvent(LearningEvent event) {
        if (!kafkaEnabled || kafkaTemplate == null) {
            log.debug("Kafka disabled or unavailable; skipping message dispatch to [{}] for user [{}]",
                    TOPIC_LEARNING_EVENTS, event.userId());
            return;
        }

        try {
            String key = event.userId().toString();
            kafkaTemplate.send(TOPIC_LEARNING_EVENTS, key, event);
            log.info("Published LearningEvent for user [{}] with mistake [{}]", event.userId(), event.mistakeType());
        } catch (Exception ex) {
            log.error("Unexpected error publishing LearningEvent for user [{}]", event.userId(), ex);
        }
    }
}
