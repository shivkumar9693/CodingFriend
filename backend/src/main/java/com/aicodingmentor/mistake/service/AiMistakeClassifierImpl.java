package com.aicodingmentor.mistake.service;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.execution.model.SubmissionEvaluationResult;
import com.aicodingmentor.execution.model.TestCaseEvaluationResult;
import com.aicodingmentor.mentor.service.MentorModelClient;
import com.aicodingmentor.mentor.service.PromptSanitizer;
import com.aicodingmentor.mistake.model.MistakeAnalysis;
import com.aicodingmentor.mistake.model.MistakeSeverity;
import com.aicodingmentor.mistake.model.MistakeType;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.submission.domain.Submission;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AiMistakeClassifierImpl implements AiMistakeClassifier {

    private static final Logger log = LoggerFactory.getLogger(AiMistakeClassifierImpl.class);
    private static final double CONFIDENCE_THRESHOLD = 0.60;

    private final MistakeAnalyzer deterministicAnalyzer;
    private final MentorModelClient mentorModelClient;
    private final PromptSanitizer promptSanitizer;
    private final ObjectMapper objectMapper;

    public AiMistakeClassifierImpl(
            MistakeAnalyzer deterministicAnalyzer,
            MentorModelClient mentorModelClient,
            PromptSanitizer promptSanitizer,
            ObjectMapper objectMapper
    ) {
        this.deterministicAnalyzer = deterministicAnalyzer;
        this.mentorModelClient = mentorModelClient;
        this.promptSanitizer = promptSanitizer;
        this.objectMapper = objectMapper;
    }

    @Override
    public MistakeAnalysis classify(Problem problem, Submission submission, SubmissionEvaluationResult executionResults) {
        if (executionResults == null) {
            return MistakeAnalysis.of(
                    MistakeType.UNKNOWN,
                    "No execution evaluation results available.",
                    "",
                    MistakeSeverity.LOW,
                    0.0
            );
        }

        EvaluationVerdict verdict = executionResults.verdict() != null
                ? executionResults.verdict()
                : EvaluationVerdict.ACCEPTED;

        // CARDINAL RULE: Deterministic evidence takes absolute precedence!
        // Never override compilation failure, runtime exceptions, or timeouts with AI guesses.
        if (verdict != EvaluationVerdict.WRONG_ANSWER) {
            log.debug("Deterministic verdict [{}] detected. Skipping AI inference and applying deterministic analysis.", verdict);
            return deterministicAnalyzer.analyze(problem, submission, executionResults);
        }

        // For WRONG_ANSWER (logical bug), consult AI classification with confidence checks
        String systemPrompt = """
                You are an expert diagnostic classifier for code execution logical failures.
                Analyze the learner's code and execution evidence to classify the bug into one of:
                - OFF_BY_ONE
                - WRONG_CONDITION
                - EDGE_CASE
                - INEFFICIENT_ALGORITHM
                - INCORRECT_DATA_STRUCTURE
                - RECURSION_ERROR
                - LOGICAL_ERROR
                - OTHER
                
                Respond ONLY in valid JSON format:
                {
                  "mistakeType": "OFF_BY_ONE|WRONG_CONDITION|EDGE_CASE|INEFFICIENT_ALGORITHM|INCORRECT_DATA_STRUCTURE|RECURSION_ERROR|LOGICAL_ERROR|OTHER",
                  "confidence": 0.85,
                  "explanation": "Short diagnostic explanation of the logical discrepancy"
                }
                """;

        String userPrompt = buildClassificationUserPrompt(problem, submission, executionResults);

        try {
            Optional<String> aiResponseOpt = mentorModelClient.generate(systemPrompt, userPrompt);
            if (aiResponseOpt.isPresent()) {
                JsonNode json = objectMapper.readTree(aiResponseOpt.get());

                double confidence = json.has("confidence") ? json.get("confidence").asDouble() : 0.50;
                String explanation = json.has("explanation") ? json.get("explanation").asText() : "Logical discrepancy in output.";
                String mistakeTypeStr = json.has("mistakeType") ? json.get("mistakeType").asText().toUpperCase() : "UNKNOWN";

                // If confidence is low, return UNKNOWN rather than inventing an explanation
                if (confidence < CONFIDENCE_THRESHOLD) {
                    log.info("AI classification confidence [{}] below threshold [{}]. Returning UNKNOWN.",
                            confidence, CONFIDENCE_THRESHOLD);
                    return MistakeAnalysis.of(
                            MistakeType.UNKNOWN,
                            "Uncertain logical error pattern (low diagnostic confidence). Please review failing test cases.",
                            explanation,
                            MistakeSeverity.MEDIUM,
                            confidence
                    );
                }

                MistakeType mistakeType = parseMistakeType(mistakeTypeStr);
                log.info("AI successfully classified logical error as [{}] with confidence [{}]", mistakeType, confidence);

                return MistakeAnalysis.of(
                        mistakeType,
                        explanation,
                        "",
                        MistakeSeverity.MEDIUM,
                        confidence
                );
            }
        } catch (Exception ex) {
            log.warn("Error during AI mistake classification: {}. Falling back to deterministic analysis.", ex.getMessage());
        }

        // Safe deterministic fallback when AI is unavailable or malformed
        return deterministicAnalyzer.analyze(problem, submission, executionResults);
    }

    private String buildClassificationUserPrompt(Problem problem, Submission submission, SubmissionEvaluationResult results) {
        StringBuilder sb = new StringBuilder();
        if (problem != null) {
            sb.append("PROBLEM: ").append(promptSanitizer.sanitize(problem.getTitle())).append("\n");
            sb.append("DESCRIPTION: ").append(promptSanitizer.sanitize(problem.getDescription())).append("\n\n");
        }

        if (submission != null) {
            sb.append("CODE:\n```java\n").append(promptSanitizer.sanitize(submission.getSourceCode())).append("\n```\n\n");
        }

        if (results.testCaseResults() != null) {
            for (TestCaseEvaluationResult tc : results.testCaseResults()) {
                if (tc.verdict() == EvaluationVerdict.WRONG_ANSWER) {
                    String sanitizedFailure = promptSanitizer.sanitizeTestFailure(
                            tc.input(),
                            tc.expectedOutput(),
                            tc.actualOutput(),
                            tc.isHidden()
                    );
                    sb.append("FAILING TEST:\n").append(sanitizedFailure).append("\n");
                    break;
                }
            }
        }

        return sb.toString();
    }

    private MistakeType parseMistakeType(String typeStr) {
        try {
            return MistakeType.valueOf(typeStr);
        } catch (IllegalArgumentException ex) {
            return MistakeType.LOGICAL_ERROR;
        }
    }
}
