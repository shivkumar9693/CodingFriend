package com.aicodingmentor.execution.service;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.execution.model.SubmissionEvaluationResult;
import com.aicodingmentor.execution.model.TestCaseEvaluationResult;
import com.aicodingmentor.problem.domain.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TestCaseEvaluator Unit & Integration Tests")
class TestCaseEvaluatorTest {

    private TestCaseEvaluator evaluator;

    @BeforeEach
    void setUp() {
        MultiLanguageExecutor codeExecutor = new MultiLanguageExecutor();
        OutputComparator outputComparator = new OutputComparator();
        DriverHarnessInjector injector = new DriverHarnessInjector(new DriverHarnessRegistry());
        evaluator = new TestCaseEvaluatorImpl(codeExecutor, outputComparator, injector);
    }

    @Test
    @DisplayName("Should evaluate multiple test cases and return ACCEPTED on all matching")
    void testEvaluate_allPass_returnsAccepted() {
        String source = """
                import java.util.Scanner;
                public class Solution {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int a = sc.nextInt();
                        int b = sc.nextInt();
                        System.out.println(a + b);
                    }
                }
                """;

        TestCase tc1 = new TestCase("2 3\n", "5\n", false, 1, "Sample 1");
        TestCase tc2 = new TestCase("10 20\n", "30\n", false, 2, "Sample 2");
        TestCase tc3 = new TestCase("-5 5\n", "0\n", true, 3, "Hidden 1"); // Hidden

        SubmissionEvaluationResult result = evaluator.evaluate(source, "JAVA", List.of(tc1, tc2, tc3), 5000L, 256);

        assertThat(result.verdict()).isEqualTo(EvaluationVerdict.ACCEPTED);
        assertThat(result.totalTests()).isEqualTo(3);
        assertThat(result.testsPassed()).isEqualTo(3);
        assertThat(result.compilationError()).isNull();
        assertThat(result.testCaseResults()).hasSize(3);

        // Verify hidden test privacy
        TestCaseEvaluationResult hiddenResult = result.testCaseResults().get(2);
        assertThat(hiddenResult.isHidden()).isTrue();
        assertThat(hiddenResult.passed()).isTrue();
        assertThat(hiddenResult.input()).isNull();
        assertThat(hiddenResult.expectedOutput()).isNull();
        assertThat(hiddenResult.actualOutput()).isNull();
    }

    @Test
    @DisplayName("Should return WRONG_ANSWER when actual output differs from expected")
    void testEvaluate_wrongAnswer_returnsWrongAnswer() {
        String source = """
                import java.util.Scanner;
                public class Solution {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int a = sc.nextInt();
                        int b = sc.nextInt();
                        System.out.println(a * b); // Bug: multiplied instead of added
                    }
                }
                """;

        TestCase tc1 = new TestCase("2 2\n", "4\n", false, 1, "Sample 1 (happens to pass 2*2=4)");
        TestCase tc2 = new TestCase("2 3\n", "5\n", false, 2, "Sample 2 (fails 2*3=6 != 5)");

        SubmissionEvaluationResult result = evaluator.evaluate(source, "JAVA", List.of(tc1, tc2), 5000L, 256);

        assertThat(result.verdict()).isEqualTo(EvaluationVerdict.WRONG_ANSWER);
        assertThat(result.totalTests()).isEqualTo(2);
        assertThat(result.testsPassed()).isEqualTo(1);
        assertThat(result.testCaseResults().get(0).passed()).isTrue();
        assertThat(result.testCaseResults().get(1).passed()).isFalse();
        assertThat(result.testCaseResults().get(1).verdict()).isEqualTo(EvaluationVerdict.WRONG_ANSWER);
        assertThat(result.testCaseResults().get(1).actualOutput().trim()).isEqualTo("6");
    }

    @Test
    @DisplayName("Should return COMPILE_ERROR immediately when source code has syntax errors")
    void testEvaluate_compileError_returnsCompileError() {
        String source = """
                public class BrokenSolution {
                    public static void main(String[] args) {
                        syntax error here ;;;
                    }
                }
                """;

        TestCase tc1 = new TestCase("1\n", "1\n", false, 1, "Sample 1");
        TestCase tc2 = new TestCase("2\n", "2\n", true, 2, "Hidden 1");

        SubmissionEvaluationResult result = evaluator.evaluate(source, "JAVA", List.of(tc1, tc2), 5000L, 256);

        assertThat(result.verdict()).isEqualTo(EvaluationVerdict.COMPILE_ERROR);
        assertThat(result.totalTests()).isEqualTo(0);
        assertThat(result.testsPassed()).isEqualTo(0);
        assertThat(result.compilationError()).isNotEmpty();
        assertThat(result.testCaseResults()).isEmpty();
    }

    @Test
    @DisplayName("Should return RUNTIME_ERROR when code throws an unhandled exception")
    void testEvaluate_runtimeError_returnsRuntimeError() {
        String source = """
                import java.util.Scanner;
                public class CrashSolution {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int x = sc.nextInt();
                        System.out.println(100 / x); // Throws ArithmeticException when x=0
                    }
                }
                """;

        TestCase tc1 = new TestCase("10\n", "10\n", false, 1, "Sample 1");
        TestCase tc2 = new TestCase("0\n", "Error\n", false, 2, "Sample 2 Division by Zero");

        SubmissionEvaluationResult result = evaluator.evaluate(source, "JAVA", List.of(tc1, tc2), 5000L, 256);

        assertThat(result.verdict()).isEqualTo(EvaluationVerdict.RUNTIME_ERROR);
        assertThat(result.totalTests()).isEqualTo(2);
        assertThat(result.testsPassed()).isEqualTo(1);
        assertThat(result.testCaseResults().get(1).verdict()).isEqualTo(EvaluationVerdict.RUNTIME_ERROR);
        assertThat(result.testCaseResults().get(1).errorMessage()).contains("ArithmeticException");
    }

    @Test
    @DisplayName("Should return TIME_LIMIT_EXCEEDED when code encounters infinite loop")
    void testEvaluate_timeout_returnsTimeLimitExceeded() {
        String source = """
                public class InfiniteLoopSolution {
                    public static void main(String[] args) {
                        while (true) {
                            // Infinite loop
                        }
                    }
                }
                """;

        TestCase tc1 = new TestCase("1\n", "1\n", false, 1, "Sample 1");

        SubmissionEvaluationResult result = evaluator.evaluate(source, "JAVA", List.of(tc1), 1500L, 256);

        assertThat(result.verdict()).isEqualTo(EvaluationVerdict.TIME_LIMIT_EXCEEDED);
        assertThat(result.totalTests()).isEqualTo(1);
        assertThat(result.testsPassed()).isEqualTo(0);
        assertThat(result.testCaseResults().get(0).verdict()).isEqualTo(EvaluationVerdict.TIME_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("Should preserve privacy on failed hidden test cases without leaking secret input")
    void testEvaluate_hiddenTestFailure_masksSecretData() {
        String source = """
                import java.util.Scanner;
                public class Solution {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        String s = sc.nextLine();
                        if (s.equals("SECRET_KEY")) {
                            System.out.println("WRONG");
                        } else {
                            System.out.println(s);
                        }
                    }
                }
                """;

        TestCase publicTc = new TestCase("HELLO\n", "HELLO\n", false, 1, "Sample 1");
        TestCase hiddenTc = new TestCase("SECRET_KEY\n", "SECRET_KEY\n", true, 2, "Secret Case");

        SubmissionEvaluationResult result = evaluator.evaluate(source, "JAVA", List.of(publicTc, hiddenTc), 5000L, 256);

        assertThat(result.verdict()).isEqualTo(EvaluationVerdict.WRONG_ANSWER);
        assertThat(result.totalTests()).isEqualTo(2);
        assertThat(result.testsPassed()).isEqualTo(1);

        TestCaseEvaluationResult hiddenResult = result.testCaseResults().get(1);
        assertThat(hiddenResult.isHidden()).isTrue();
        assertThat(hiddenResult.passed()).isFalse();
        assertThat(hiddenResult.input()).isNull(); // Never leak SECRET_KEY
        assertThat(hiddenResult.expectedOutput()).isNull();
        assertThat(hiddenResult.actualOutput()).isNull();
    }
}
