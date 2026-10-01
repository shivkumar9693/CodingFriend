package com.aicodingmentor.mentor.service;

import com.aicodingmentor.mentor.dto.AiPromptContext;
import com.aicodingmentor.mentor.dto.SocraBuddyDiagnosisRequest;
import com.aicodingmentor.mentor.dto.SocraBuddyHintRequest;
import com.aicodingmentor.problem.domain.Problem;

/**
 * Service contract for constructing controlled, versioned, and sanitized prompts for AI Mentor models.
 */
public interface PromptBuilder {

    /**
     * Builds the standard system prompt defining the Socratic rules and anti-spoil constraints.
     *
     * @return system prompt string
     */
    String buildSystemPrompt();

    /**
     * Builds a specific versioned system prompt.
     *
     * @param version prompt version identifier (e.g., "v1", "v2")
     * @return system prompt string for the requested version
     */
    String buildSystemPrompt(String version);

    /**
     * Constructs a hint prompt from problem statement and basic request.
     *
     * @param problem coding problem entity
     * @param request student hint request
     * @return sanitized user prompt for LLM
     */
    String buildHintUserPrompt(Problem problem, SocraBuddyHintRequest request);

    /**
     * Constructs a failure diagnosis prompt for execution mistakes.
     *
     * @param problem coding problem entity
     * @param request student failure diagnosis request
     * @return sanitized diagnosis prompt for LLM
     */
    String buildDiagnosisPrompt(Problem problem, SocraBuddyDiagnosisRequest request);

    /**
     * Constructs a comprehensive contextual prompt incorporating problem metadata,
     * learner code, mistake classifications, execution metrics, previous hints, and mistake trends.
     *
     * @param context rich prompt context
     * @return sanitized, structured user prompt for LLM
     */
    String buildContextualPrompt(AiPromptContext context);

    /**
     * Returns the active prompt template version.
     *
     * @return version string (e.g., "v1.4.0-socratic-context")
     */
    String getPromptVersion();
}
