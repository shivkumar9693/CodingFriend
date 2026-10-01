package com.aicodingmentor.submission.kafka;

import com.aicodingmentor.submission.event.AiAnalysisEvent;
import com.aicodingmentor.submission.event.CodeSubmissionEvent;
import com.aicodingmentor.submission.event.ExecutionResultEvent;
import com.aicodingmentor.submission.event.LearningEvent;

/**
 * Interface for publishing submission, execution, and learning domain events.
 */
public interface SubmissionEventProducer {

    void sendCodeSubmission(CodeSubmissionEvent event);

    void sendExecutionResult(ExecutionResultEvent event);

    void sendAiAnalysis(AiAnalysisEvent event);

    void sendLearningEvent(LearningEvent event);
}
