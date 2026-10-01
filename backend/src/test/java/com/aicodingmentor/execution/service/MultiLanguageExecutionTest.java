package com.aicodingmentor.execution.service;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.execution.model.ExecutionRequest;
import com.aicodingmentor.execution.model.ExecutionResult;
import com.aicodingmentor.execution.model.SubmissionEvaluationResult;
import com.aicodingmentor.execution.model.SupportedLanguage;
import com.aicodingmentor.problem.domain.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Multi-Language Execution & Driver Harness Tests")
class MultiLanguageExecutionTest {

    private MultiLanguageExecutor executor;
    private DriverHarnessRegistry registry;
    private DriverHarnessInjector injector;
    private TestCaseEvaluator evaluator;

    @BeforeEach
    void setUp() {
        executor = new MultiLanguageExecutor();
        registry = new DriverHarnessRegistry();
        injector = new DriverHarnessInjector(registry);
        evaluator = new TestCaseEvaluatorImpl(executor, new OutputComparator(), injector);
    }

    @Test
    @DisplayName("Should execute Python script successfully")
    void testPythonExecution() {
        String pyCode = """
                import sys
                line = sys.stdin.read().strip()
                print(f"Echo: {line}")
                """;
        ExecutionRequest request = ExecutionRequest.of(pyCode, "Hello Python", SupportedLanguage.PYTHON);
        ExecutionResult result = executor.execute(request);

        assertThat(result.success()).isTrue();
        assertThat(result.stdout().trim()).isEqualTo("Echo: Hello Python");
    }

    @Test
    @DisplayName("Should execute JavaScript (Node.js) script successfully")
    void testJavaScriptExecution() {
        String jsCode = """
                const fs = require('fs');
                const input = fs.readFileSync(0, 'utf-8').trim();
                console.log("Echo: " + input);
                """;
        ExecutionRequest request = ExecutionRequest.of(jsCode, "Hello Node", SupportedLanguage.JAVASCRIPT);
        ExecutionResult result = executor.execute(request);

        assertThat(result.success()).isTrue();
        assertThat(result.stdout().trim()).isEqualTo("Echo: Hello Node");
    }

    @Test
    @DisplayName("Should evaluate Python function-only submission for Two Sum and return ACCEPTED")
    void testPythonFunctionEvaluation_twoSum_returnsAccepted() {
        String pythonFunction = """
                class Solution:
                    def twoSum(self, nums: list[int], target: int) -> list[int]:
                        seen = {}
                        for i, num in enumerate(nums):
                            diff = target - num
                            if diff in seen:
                                return [seen[diff], i]
                            seen[num] = i
                        return []
                """;

        TestCase tc1 = new TestCase("[2, 7, 11, 15]\n9\n", "[0, 1]\n", false, 1, "Sample 1");
        TestCase tc2 = new TestCase("[3, 2, 4]\n6\n", "[1, 2]\n", false, 2, "Sample 2");

        SubmissionEvaluationResult result = evaluator.evaluate(
                pythonFunction,
                "PYTHON",
                List.of(tc1, tc2),
                5000L,
                256,
                "two-sum"
        );

        assertThat(result.verdict()).isEqualTo(EvaluationVerdict.ACCEPTED);
        assertThat(result.testsPassed()).isEqualTo(2);
        assertThat(result.totalTests()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should evaluate JavaScript function-only submission for Two Sum and return ACCEPTED")
    void testJavaScriptFunctionEvaluation_twoSum_returnsAccepted() {
        String jsFunction = """
                var twoSum = function(nums, target) {
                    const map = new Map();
                    for (let i = 0; i < nums.length; i++) {
                        const complement = target - nums[i];
                        if (map.has(complement)) {
                            return [map.get(complement), i];
                        }
                        map.set(nums[i], i);
                    }
                    return [];
                };
                """;

        TestCase tc1 = new TestCase("[2, 7, 11, 15]\n9\n", "[0, 1]\n", false, 1, "Sample 1");
        TestCase tc2 = new TestCase("[3, 2, 4]\n6\n", "[1, 2]\n", false, 2, "Sample 2");

        SubmissionEvaluationResult result = evaluator.evaluate(
                jsFunction,
                "JAVASCRIPT",
                List.of(tc1, tc2),
                5000L,
                256,
                "two-sum"
        );

        assertThat(result.verdict()).isEqualTo(EvaluationVerdict.ACCEPTED);
        assertThat(result.testsPassed()).isEqualTo(2);
        assertThat(result.totalTests()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should evaluate Java function-only submission for Two Sum and return ACCEPTED")
    void testJavaFunctionEvaluation_twoSum_returnsAccepted() {
        String javaFunction = """
                class Solution {
                    public int[] twoSum(int[] nums, int target) {
                        Map<Integer, Integer> map = new HashMap<>();
                        for (int i = 0; i < nums.length; i++) {
                            int complement = target - nums[i];
                            if (map.containsKey(complement)) {
                                return new int[] { map.get(complement), i };
                            }
                            map.put(nums[i], i);
                        }
                        return new int[]{};
                    }
                }
                """;

        TestCase tc1 = new TestCase("[2, 7, 11, 15]\n9\n", "[0, 1]\n", false, 1, "Sample 1");
        TestCase tc2 = new TestCase("[3, 2, 4]\n6\n", "[1, 2]\n", false, 2, "Sample 2");

        SubmissionEvaluationResult result = evaluator.evaluate(
                javaFunction,
                "JAVA",
                List.of(tc1, tc2),
                5000L,
                256,
                "two-sum"
        );

        assertThat(result.verdict()).isEqualTo(EvaluationVerdict.ACCEPTED);
        assertThat(result.testsPassed()).isEqualTo(2);
        assertThat(result.totalTests()).isEqualTo(2);
    }
}
