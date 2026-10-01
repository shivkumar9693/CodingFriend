package com.aicodingmentor.mistake.service;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.execution.model.SubmissionEvaluationResult;
import com.aicodingmentor.execution.model.TestCaseEvaluationResult;
import com.aicodingmentor.mentor.service.MentorModelClient;
import com.aicodingmentor.mentor.service.PromptSanitizer;
import com.aicodingmentor.mistake.model.MistakeAnalysis;
import com.aicodingmentor.mistake.model.MistakeType;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.submission.domain.Submission;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiMistakeClassifier Unit Tests")
class AiMistakeClassifierTest {

    @Mock
    private MistakeAnalyzer deterministicAnalyzer;

    @Mock
    private MentorModelClient mentorModelClient;

    private PromptSanitizer promptSanitizer;
    private ObjectMapper objectMapper;
    private AiMistakeClassifierImpl classifier;

    private Problem sampleProblem;
    private Submission sampleSubmission;

    @BeforeEach
    void setUp() {
        promptSanitizer = new PromptSanitizer();
        objectMapper = new ObjectMapper();
        classifier = new AiMistakeClassifierImpl(
                deterministicAnalyzer,
                mentorModelClient,
                promptSanitizer,
                objectMapper
        );

        sampleProblem = new Problem(
                "Two Sum", "two-sum", "Find indices.", ProblemDifficulty.EASY,
                "2 <= n <= 100", "int[] nums, int target", "int[]",
                "example", "starter", "JAVA", Set.of("ARRAY")
        );
        sampleProblem.setId(UUID.randomUUID());

        sampleSubmission = new Submission(
                UUID.randomUUID(), sampleProblem.getId(),
                "class Solution { public int[] twoSum() {} }", "JAVA"
        );
    }

    @Test
    @DisplayName("Deterministic compile error must take absolute precedence and bypass AI")
    void testDeterministicCompileErrorTakesPrecedence() {
        SubmissionEvaluationResult compileFailure = new SubmissionEvaluationResult(
                EvaluationVerdict.COMPILE_ERROR,
                0, 5, 0L, "Solution.java:3: error: ';' expected", List.of()
        );

        when(deterministicAnalyzer.analyze(any(), any(Submission.class), any()))
                .thenReturn(MistakeAnalysis.compileError("Syntax error", "Solution.java:3"));

        MistakeAnalysis analysis = classifier.classify(sampleProblem, sampleSubmission, compileFailure);

        assertThat(analysis.type()).isEqualTo(MistakeType.COMPILE_ERROR);
        verify(mentorModelClient, never()).generate(anyString(), anyString());
    }

    @Test
    @DisplayName("Deterministic runtime exception must take precedence and bypass AI")
    void testDeterministicRuntimeErrorTakesPrecedence() {
        SubmissionEvaluationResult runtimeFailure = new SubmissionEvaluationResult(
                EvaluationVerdict.RUNTIME_ERROR,
                1, 5, 15L, "java.lang.NullPointerException at line 14", List.of()
        );

        when(deterministicAnalyzer.analyze(any(), any(Submission.class), any()))
                .thenReturn(MistakeAnalysis.of(MistakeType.NULL_POINTER, "Null pointer", "", com.aicodingmentor.mistake.model.MistakeSeverity.HIGH, 1.0));

        MistakeAnalysis analysis = classifier.classify(sampleProblem, sampleSubmission, runtimeFailure);

        assertThat(analysis.type()).isEqualTo(MistakeType.NULL_POINTER);
        verify(mentorModelClient, never()).generate(anyString(), anyString());
    }

    @Test
    @DisplayName("Valid AI classification on logical WRONG_ANSWER with high confidence")
    void testValidAiLogicalClassification() {
        TestCaseEvaluationResult failedTest = new TestCaseEvaluationResult(
                1, false, false, EvaluationVerdict.WRONG_ANSWER, "[2, 7], target=9", "[0, 1]", "[]", 5L, null
        );
        SubmissionEvaluationResult wrongAnswer = new SubmissionEvaluationResult(
                EvaluationVerdict.WRONG_ANSWER,
                0, 1, 5L, null, List.of(failedTest)
        );

        String aiJson = """
                {
                  "mistakeType": "OFF_BY_ONE",
                  "confidence": 0.88,
                  "explanation": "The loop index stops at nums.length - 2, missing the final element pair."
                }
                """;
        when(mentorModelClient.generate(anyString(), anyString())).thenReturn(Optional.of(aiJson));

        MistakeAnalysis analysis = classifier.classify(sampleProblem, sampleSubmission, wrongAnswer);

        assertThat(analysis.type()).isEqualTo(MistakeType.OFF_BY_ONE);
        assertThat(analysis.confidence()).isEqualTo(0.88);
        assertThat(analysis.message()).contains("missing the final element pair");
    }

    @Test
    @DisplayName("Low confidence (< 0.60) returns UNKNOWN rather than inventing an explanation")
    void testLowConfidenceReturnsUnknown() {
        TestCaseEvaluationResult failedTest = new TestCaseEvaluationResult(
                1, false, false, EvaluationVerdict.WRONG_ANSWER, "[1, 2, 3]", "6", "5", 5L, null
        );
        SubmissionEvaluationResult wrongAnswer = new SubmissionEvaluationResult(
                EvaluationVerdict.WRONG_ANSWER,
                0, 1, 5L, null, List.of(failedTest)
        );

        String aiJson = """
                {
                  "mistakeType": "EDGE_CASE",
                  "confidence": 0.45,
                  "explanation": "Uncertain if loop condition or math operator is incorrect."
                }
                """;
        when(mentorModelClient.generate(anyString(), anyString())).thenReturn(Optional.of(aiJson));

        MistakeAnalysis analysis = classifier.classify(sampleProblem, sampleSubmission, wrongAnswer);

        assertThat(analysis.type()).isEqualTo(MistakeType.UNKNOWN);
        assertThat(analysis.confidence()).isEqualTo(0.45);
        assertThat(analysis.message()).contains("Uncertain logical error pattern");
    }

    @Test
    @DisplayName("Malformed AI response falls back to deterministic analysis gracefully")
    void testMalformedAiResponseFallsBackGracefully() {
        SubmissionEvaluationResult wrongAnswer = new SubmissionEvaluationResult(
                EvaluationVerdict.WRONG_ANSWER,
                0, 1, 5L, null, List.of()
        );

        when(mentorModelClient.generate(anyString(), anyString())).thenReturn(Optional.of("NOT_JSON_PAYLOAD"));
        when(deterministicAnalyzer.analyze(any(), any(Submission.class), any()))
                .thenReturn(MistakeAnalysis.of(MistakeType.WRONG_ANSWER, "Output mismatch", "", com.aicodingmentor.mistake.model.MistakeSeverity.MEDIUM, 0.70));

        MistakeAnalysis analysis = classifier.classify(sampleProblem, sampleSubmission, wrongAnswer);

        assertThat(analysis.type()).isEqualTo(MistakeType.WRONG_ANSWER);
        verify(deterministicAnalyzer, times(1)).analyze(any(), any(Submission.class), any());
    }

    @Test
    @DisplayName("AI unavailable falls back to deterministic analysis gracefully")
    void testAiUnavailableFallsBackGracefully() {
        SubmissionEvaluationResult wrongAnswer = new SubmissionEvaluationResult(
                EvaluationVerdict.WRONG_ANSWER,
                0, 1, 5L, null, List.of()
        );

        when(mentorModelClient.generate(anyString(), anyString())).thenReturn(Optional.empty());
        when(deterministicAnalyzer.analyze(any(), any(Submission.class), any()))
                .thenReturn(MistakeAnalysis.of(MistakeType.WRONG_ANSWER, "Output mismatch", "", com.aicodingmentor.mistake.model.MistakeSeverity.MEDIUM, 0.70));

        MistakeAnalysis analysis = classifier.classify(sampleProblem, sampleSubmission, wrongAnswer);

        assertThat(analysis.type()).isEqualTo(MistakeType.WRONG_ANSWER);
        verify(deterministicAnalyzer, times(1)).analyze(any(), any(Submission.class), any());
    }
}
