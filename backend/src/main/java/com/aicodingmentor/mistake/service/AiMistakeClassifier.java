package com.aicodingmentor.mistake.service;

import com.aicodingmentor.execution.model.SubmissionEvaluationResult;
import com.aicodingmentor.mistake.model.MistakeAnalysis;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.submission.domain.Submission;

/**
 * AI-assisted classifier for diagnosing high-level algorithmic and logical mistakes
 * while strictly honoring deterministic execution evidence.
 */
public interface AiMistakeClassifier {

    /**
     * Classifies a submission failure, giving deterministic evidence precedence and
     * using LLM inference with confidence scoring for logical errors.
     *
     * @param problem problem statement
     * @param submission user submission entity
     * @param executionResults execution outcome and test results
     * @return structured MistakeAnalysis
     */
    MistakeAnalysis classify(Problem problem, Submission submission, SubmissionEvaluationResult executionResults);
}
