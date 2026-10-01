package com.aicodingmentor.mistake.service;

import com.aicodingmentor.execution.model.SubmissionEvaluationResult;
import com.aicodingmentor.mistake.model.MistakeAnalysis;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.submission.domain.Submission;

public interface MistakeAnalyzer {

    /**
     * Perform deterministic mistake analysis on a submission and its execution results without using AI.
     *
     * @param problem the target problem statement and metadata
     * @param submission the user's submission entity containing source code
     * @param executionResults the structured execution results across all evaluated test cases
     * @return structured MistakeAnalysis with type, message, evidence, severity, and confidence
     */
    MistakeAnalysis analyze(Problem problem, Submission submission, SubmissionEvaluationResult executionResults);

    /**
     * Overloaded helper for analyzing raw source code and execution results directly.
     */
    MistakeAnalysis analyze(Problem problem, String sourceCode, SubmissionEvaluationResult executionResults);
}
