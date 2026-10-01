package com.aicodingmentor.mistake.service;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.execution.model.SubmissionEvaluationResult;
import com.aicodingmentor.execution.model.TestCaseEvaluationResult;
import com.aicodingmentor.mistake.model.MistakeAnalysis;
import com.aicodingmentor.mistake.model.MistakeSeverity;
import com.aicodingmentor.mistake.model.MistakeType;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.submission.domain.Submission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MistakeAnalyzer Unit Tests")
class MistakeAnalyzerTest {

    private MistakeAnalyzer analyzer;
    private Problem problem;

    @BeforeEach
    void setUp() {
        analyzer = new MistakeAnalyzerImpl();
        problem = new Problem(
                "Two Sum",
                "two-sum",
                "Given an array of integers...",
                ProblemDifficulty.EASY,
                "Constraints",
                "Input",
                "Output",
                "Examples",
                "class Solution {}",
                "JAVA",
                Set.of("Array")
        );
        problem.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should detect Compilation Error with line number and syntax error message")
    void testAnalyze_compilationError() {
        String compileError = """
                Solution.java:12: error: ';' expected
                    int x = 5
                             ^
                1 error
                """;

        SubmissionEvaluationResult evalResult = SubmissionEvaluationResult.compileError(compileError, 45);
        MistakeAnalysis analysis = analyzer.analyze(problem, "int x = 5", evalResult);

        assertThat(analysis.type()).isEqualTo(MistakeType.COMPILE_ERROR);
        assertThat(analysis.severity()).isEqualTo(MistakeSeverity.CRITICAL);
        assertThat(analysis.confidence()).isEqualTo(1.0);
        assertThat(analysis.message()).contains("line 12");
        assertThat(analysis.message()).contains("';' expected");
        assertThat(analysis.evidence()).contains("Solution.java:12");
    }

    @Test
    @DisplayName("Should detect ArrayIndexOutOfBoundsException with index details and stack location")
    void testAnalyze_arrayIndexOutOfBounds() {
        String stderr = """
                Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: Index 5 out of bounds for length 5
                    at Solution.twoSum(Solution.java:18)
                    at Solution.main(Solution.java:25)
                """;

        TestCaseEvaluationResult tc = TestCaseEvaluationResult.failed(
                1, false, EvaluationVerdict.RUNTIME_ERROR, "5", "10", "", 40, stderr
        );
        SubmissionEvaluationResult evalResult = SubmissionEvaluationResult.of(
                EvaluationVerdict.RUNTIME_ERROR, 1, 0, 40, List.of(tc)
        );

        MistakeAnalysis analysis = analyzer.analyze(problem, "class Solution {}", evalResult);

        assertThat(analysis.type()).isEqualTo(MistakeType.ARRAY_INDEX_OUT_OF_BOUNDS);
        assertThat(analysis.severity()).isEqualTo(MistakeSeverity.HIGH);
        assertThat(analysis.message()).contains("ArrayIndexOutOfBoundsException");
        assertThat(analysis.message()).contains("Solution.java:18");
        assertThat(analysis.evidence()).contains("ArrayIndexOutOfBoundsException");
    }

    @Test
    @DisplayName("Should detect NullPointerException")
    void testAnalyze_nullPointer() {
        String stderr = """
                Exception in thread "main" java.lang.NullPointerException: Cannot invoke "String.length()" because "s" is null
                    at Solution.solve(Solution.java:8)
                    at Solution.main(Solution.java:14)
                """;

        TestCaseEvaluationResult tc = TestCaseEvaluationResult.failed(
                1, false, EvaluationVerdict.RUNTIME_ERROR, "input", "output", "", 30, stderr
        );
        SubmissionEvaluationResult evalResult = SubmissionEvaluationResult.of(
                EvaluationVerdict.RUNTIME_ERROR, 1, 0, 30, List.of(tc)
        );

        MistakeAnalysis analysis = analyzer.analyze(problem, "class Solution {}", evalResult);

        assertThat(analysis.type()).isEqualTo(MistakeType.NULL_POINTER);
        assertThat(analysis.severity()).isEqualTo(MistakeSeverity.HIGH);
        assertThat(analysis.message()).contains("NullPointerException");
        assertThat(analysis.message()).contains("Solution.java:8");
    }

    @Test
    @DisplayName("Should detect NumberFormatException")
    void testAnalyze_numberFormatException() {
        String stderr = """
                Exception in thread "main" java.lang.NumberFormatException: For input string: "abc"
                    at java.base/java.lang.NumberFormatException.forInputString(NumberFormatException.java:67)
                    at java.base/java.lang.Integer.parseInt(Integer.java:668)
                    at Solution.main(Solution.java:7)
                """;

        TestCaseEvaluationResult tc = TestCaseEvaluationResult.failed(
                1, false, EvaluationVerdict.RUNTIME_ERROR, "abc", "0", "", 35, stderr
        );
        SubmissionEvaluationResult evalResult = SubmissionEvaluationResult.of(
                EvaluationVerdict.RUNTIME_ERROR, 1, 0, 35, List.of(tc)
        );

        MistakeAnalysis analysis = analyzer.analyze(problem, "class Solution {}", evalResult);

        assertThat(analysis.type()).isEqualTo(MistakeType.NUMBER_FORMAT);
        assertThat(analysis.severity()).isEqualTo(MistakeSeverity.HIGH);
        assertThat(analysis.message()).contains("NumberFormatException");
        assertThat(analysis.message()).contains("\"abc\"");
    }

    @Test
    @DisplayName("Should detect ArithmeticException (division by zero)")
    void testAnalyze_arithmeticException() {
        String stderr = """
                Exception in thread "main" java.lang.ArithmeticException: / by zero
                    at Solution.divide(Solution.java:5)
                    at Solution.main(Solution.java:10)
                """;

        TestCaseEvaluationResult tc = TestCaseEvaluationResult.failed(
                1, false, EvaluationVerdict.RUNTIME_ERROR, "0", "0", "", 25, stderr
        );
        SubmissionEvaluationResult evalResult = SubmissionEvaluationResult.of(
                EvaluationVerdict.RUNTIME_ERROR, 1, 0, 25, List.of(tc)
        );

        MistakeAnalysis analysis = analyzer.analyze(problem, "class Solution {}", evalResult);

        assertThat(analysis.type()).isEqualTo(MistakeType.ARITHMETIC_EXCEPTION);
        assertThat(analysis.severity()).isEqualTo(MistakeSeverity.HIGH);
        assertThat(analysis.message()).contains("Division or modulo by zero");
    }

    @Test
    @DisplayName("Should detect StackOverflowError from recursive call")
    void testAnalyze_stackOverflow() {
        String stderr = """
                Exception in thread "main" java.lang.StackOverflowError
                    at Solution.recurse(Solution.java:4)
                    at Solution.recurse(Solution.java:4)
                """;

        TestCaseEvaluationResult tc = TestCaseEvaluationResult.failed(
                1, false, EvaluationVerdict.RUNTIME_ERROR, "1", "1", "", 50, stderr
        );
        SubmissionEvaluationResult evalResult = SubmissionEvaluationResult.of(
                EvaluationVerdict.RUNTIME_ERROR, 1, 0, 50, List.of(tc)
        );

        MistakeAnalysis analysis = analyzer.analyze(problem, "class Solution {}", evalResult);

        assertThat(analysis.type()).isEqualTo(MistakeType.STACK_OVERFLOW);
        assertThat(analysis.message()).contains("StackOverflowError");
    }

    @Test
    @DisplayName("Should detect Infinite Loop when code contains while(true) and encounters timeout")
    void testAnalyze_infiniteLoopTimeout() {
        String code = """
                public class Solution {
                    public static void main(String[] args) {
                        while (true) {
                            // Infinite
                        }
                    }
                }
                """;

        TestCaseEvaluationResult tc = TestCaseEvaluationResult.failed(
                1, false, EvaluationVerdict.TIME_LIMIT_EXCEEDED, "1", "1", "", 5000, "Time limit exceeded"
        );
        SubmissionEvaluationResult evalResult = SubmissionEvaluationResult.of(
                EvaluationVerdict.TIME_LIMIT_EXCEEDED, 1, 0, 5000, List.of(tc)
        );

        MistakeAnalysis analysis = analyzer.analyze(problem, code, evalResult);

        assertThat(analysis.type()).isEqualTo(MistakeType.INFINITE_LOOP);
        assertThat(analysis.severity()).isEqualTo(MistakeSeverity.HIGH);
        assertThat(analysis.message()).contains("infinite loop");
    }

    @Test
    @DisplayName("Should detect Memory Limit Exceeded")
    void testAnalyze_memoryLimit() {
        SubmissionEvaluationResult evalResult = SubmissionEvaluationResult.of(
                EvaluationVerdict.MEMORY_LIMIT_EXCEEDED, 1, 0, 200, List.of()
        );

        MistakeAnalysis analysis = analyzer.analyze(problem, "class Solution {}", evalResult);

        assertThat(analysis.type()).isEqualTo(MistakeType.MEMORY_LIMIT);
        assertThat(analysis.severity()).isEqualTo(MistakeSeverity.CRITICAL);
        assertThat(analysis.message()).contains("256MB");
    }

    @Test
    @DisplayName("Should detect Output Mismatch on public test case with expected vs actual evidence")
    void testAnalyze_wrongAnswer_publicTestCase() {
        TestCaseEvaluationResult tc1 = TestCaseEvaluationResult.passed(1, false, "2 3\n", "5\n", "5\n", 40);
        TestCaseEvaluationResult tc2 = TestCaseEvaluationResult.failed(
                2, false, EvaluationVerdict.WRONG_ANSWER, "4 5\n", "9\n", "20\n", 45, "Output does not match"
        );

        SubmissionEvaluationResult evalResult = SubmissionEvaluationResult.of(
                EvaluationVerdict.WRONG_ANSWER, 2, 1, 85, List.of(tc1, tc2)
        );

        MistakeAnalysis analysis = analyzer.analyze(problem, "class Solution {}", evalResult);

        assertThat(analysis.type()).isEqualTo(MistakeType.INCORRECT_OUTPUT);
        assertThat(analysis.severity()).isEqualTo(MistakeSeverity.MEDIUM);
        assertThat(analysis.message()).contains("test case #2");
        assertThat(analysis.message()).contains("Expected [9] but got [20]");
        assertThat(analysis.evidence()).contains("Expected: 9");
        assertThat(analysis.evidence()).contains("Actual: 20");
    }

    @Test
    @DisplayName("Should mask evidence on hidden test case failure to prevent secret leakage")
    void testAnalyze_wrongAnswer_hiddenTestCase() {
        TestCaseEvaluationResult publicTc = TestCaseEvaluationResult.passed(1, false, "2 3\n", "5\n", "5\n", 40);
        TestCaseEvaluationResult hiddenTc = TestCaseEvaluationResult.failed(
                2, true, EvaluationVerdict.WRONG_ANSWER, "SECRET_INPUT", "SECRET_EXP", "SECRET_ACT", 50, "Hidden failed"
        );

        SubmissionEvaluationResult evalResult = SubmissionEvaluationResult.of(
                EvaluationVerdict.WRONG_ANSWER, 2, 1, 90, List.of(publicTc, hiddenTc)
        );

        MistakeAnalysis analysis = analyzer.analyze(problem, "class Solution {}", evalResult);

        assertThat(analysis.type()).isEqualTo(MistakeType.INCORRECT_OUTPUT);
        assertThat(analysis.message()).contains("hidden test case #2");
        assertThat(analysis.evidence()).doesNotContain("SECRET_INPUT");
        assertThat(analysis.evidence()).doesNotContain("SECRET_EXP");
        assertThat(analysis.evidence()).isEqualTo("Hidden test assertion failed.");
    }

    @Test
    @DisplayName("Should return NONE when all test cases passed (ACCEPTED)")
    void testAnalyze_accepted() {
        TestCaseEvaluationResult tc = TestCaseEvaluationResult.passed(1, false, "1\n", "1\n", "1\n", 30);
        SubmissionEvaluationResult evalResult = SubmissionEvaluationResult.of(
                EvaluationVerdict.ACCEPTED, 1, 1, 30, List.of(tc)
        );

        MistakeAnalysis analysis = analyzer.analyze(problem, "class Solution {}", evalResult);

        assertThat(analysis.type()).isEqualTo(MistakeType.NONE);
        assertThat(analysis.severity()).isEqualTo(MistakeSeverity.LOW);
        assertThat(analysis.message()).contains("All test cases passed");
    }

    @Test
    @DisplayName("Should analyze via Submission entity overload")
    void testAnalyze_submissionEntityOverload() {
        UUID userId = UUID.randomUUID();
        Submission submission = new Submission(userId, problem.getId(), "class Solution {}", "JAVA");

        TestCaseEvaluationResult tc = TestCaseEvaluationResult.passed(1, false, "1\n", "1\n", "1\n", 30);
        SubmissionEvaluationResult evalResult = SubmissionEvaluationResult.of(
                EvaluationVerdict.ACCEPTED, 1, 1, 30, List.of(tc)
        );

        MistakeAnalysis analysis = analyzer.analyze(problem, submission, evalResult);

        assertThat(analysis.type()).isEqualTo(MistakeType.NONE);
    }
}
