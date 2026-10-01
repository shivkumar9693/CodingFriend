package com.aicodingmentor.mistake.service;

import com.aicodingmentor.execution.model.EvaluationVerdict;
import com.aicodingmentor.execution.model.SubmissionEvaluationResult;
import com.aicodingmentor.execution.model.TestCaseEvaluationResult;
import com.aicodingmentor.mistake.model.MistakeAnalysis;
import com.aicodingmentor.mistake.model.MistakeSeverity;
import com.aicodingmentor.mistake.model.MistakeType;
import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.submission.domain.Submission;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MistakeAnalyzerImpl implements MistakeAnalyzer {

    private static final Pattern COMPILER_LINE_PATTERN = Pattern.compile("(?:.*\\.java:(\\d+):\\s*error:\\s*([^\n\r]+))");
    private static final Pattern ARRAY_INDEX_PATTERN = Pattern.compile("(?:(?:Array)?IndexOutOfBoundsException)(?::\\s*([^\n\r]+))?");
    private static final Pattern NULL_POINTER_PATTERN = Pattern.compile("NullPointerException(?::\s*([^\n\r]+))?");
    private static final Pattern NUMBER_FORMAT_PATTERN = Pattern.compile("NumberFormatException(?::\s*([^\n\r]+))?");
    private static final Pattern ARITHMETIC_PATTERN = Pattern.compile("ArithmeticException(?::\s*([^\n\r]+))?");
    private static final Pattern STACK_OVERFLOW_PATTERN = Pattern.compile("StackOverflowError");
    private static final Pattern STACK_TRACE_LOCATION_PATTERN = Pattern.compile("at\\s+([A-Za-z0-9_.$]+\\([A-Za-z0-9_]+\\.java:\\d+\\))");
    private static final Pattern INFINITE_LOOP_SYNTAX_PATTERN = Pattern.compile("(?:while\\s*\\(\\s*true\\s*\\)|while\\s*\\([^)]+\\)|for\\s*\\([^;]*;[^;]*;\\s*\\))");

    @Override
    public MistakeAnalysis analyze(Problem problem, Submission submission, SubmissionEvaluationResult executionResults) {
        String sourceCode = submission != null ? submission.getSourceCode() : null;
        return analyze(problem, sourceCode, executionResults);
    }

    @Override
    public MistakeAnalysis analyze(Problem problem, String sourceCode, SubmissionEvaluationResult executionResults) {
        if (executionResults == null) {
            return MistakeAnalysis.of(
                    MistakeType.UNKNOWN,
                    "No execution results available for analysis.",
                    "",
                    MistakeSeverity.LOW,
                    0.0
            );
        }

        EvaluationVerdict verdict = executionResults.verdict();
        if (verdict == null) {
            verdict = EvaluationVerdict.ACCEPTED;
        }

        return switch (verdict) {
            case COMPILE_ERROR -> analyzeCompilationError(executionResults.compilationError());
            case TIME_LIMIT_EXCEEDED -> analyzeTimeout(sourceCode, executionResults);
            case MEMORY_LIMIT_EXCEEDED -> analyzeMemoryLimit(executionResults);
            case RUNTIME_ERROR -> analyzeRuntimeError(executionResults);
            case WRONG_ANSWER -> analyzeWrongAnswer(executionResults);
            case ACCEPTED -> MistakeAnalysis.none("All " + executionResults.totalTests() + " test cases passed.");
        };
    }

    private MistakeAnalysis analyzeCompilationError(String compileError) {
        if (compileError == null || compileError.isBlank()) {
            return MistakeAnalysis.compileError("Compilation failed with unspecified syntax errors.", "");
        }

        Matcher matcher = COMPILER_LINE_PATTERN.matcher(compileError);
        if (matcher.find()) {
            String lineNum = matcher.group(1);
            String errorMsg = matcher.group(2).trim();
            String message = "Compilation error at line " + lineNum + ": " + errorMsg;
            return MistakeAnalysis.compileError(message, compileError.trim());
        }

        // Fallback to first non-empty line
        String firstLine = compileError.lines().filter(l -> !l.isBlank()).findFirst().orElse("Compilation syntax error");
        return MistakeAnalysis.compileError("Compilation failed: " + firstLine, compileError.trim());
    }

    private MistakeAnalysis analyzeTimeout(String sourceCode, SubmissionEvaluationResult results) {
        boolean hasLoop = sourceCode != null && INFINITE_LOOP_SYNTAX_PATTERN.matcher(sourceCode).find();
        String message = hasLoop
                ? "Time limit exceeded. The program likely encountered an infinite loop or non-terminating iteration."
                : "Time limit exceeded. The algorithm's time complexity may be too high for larger test inputs.";

        String evidence = "Execution wall-clock time exceeded " + results.totalExecutionTimeMs() + "ms limit.";
        return MistakeAnalysis.timeout(message, evidence, hasLoop);
    }

    private MistakeAnalysis analyzeMemoryLimit(SubmissionEvaluationResult results) {
        return MistakeAnalysis.memoryLimit(
                "Memory limit exceeded (256MB cap). Excessive object creation or unbounded collection storage.",
                "Process exceeded memory threshold."
        );
    }

    private MistakeAnalysis analyzeRuntimeError(SubmissionEvaluationResult results) {
        String combinedError = collectRuntimeErrorOutput(results);

        // 1. ArrayIndexOutOfBoundsException
        Matcher arrayMatcher = ARRAY_INDEX_PATTERN.matcher(combinedError);
        if (arrayMatcher.find()) {
            String details = arrayMatcher.group(1);
            String location = extractStackLocation(combinedError);
            String message = (details != null && !details.isBlank())
                    ? "ArrayIndexOutOfBoundsException: Index " + details.trim() + " is outside valid array bounds."
                    : "ArrayIndexOutOfBoundsException: Attempted to access an array index outside valid range [0, length - 1].";

            if (location != null) {
                message += " (" + location + ")";
            }
            return MistakeAnalysis.runtimeError(MistakeType.ARRAY_INDEX_OUT_OF_BOUNDS, message, combinedError.trim());
        }

        // 2. NullPointerException
        Matcher nullMatcher = NULL_POINTER_PATTERN.matcher(combinedError);
        if (nullMatcher.find()) {
            String location = extractStackLocation(combinedError);
            String message = "NullPointerException: Attempted to invoke a method or access a field on a null object reference.";
            if (location != null) {
                message += " (" + location + ")";
            }
            return MistakeAnalysis.runtimeError(MistakeType.NULL_POINTER, message, combinedError.trim());
        }

        // 3. NumberFormatException
        Matcher numFormatMatcher = NUMBER_FORMAT_PATTERN.matcher(combinedError);
        if (numFormatMatcher.find()) {
            String details = numFormatMatcher.group(1);
            String message = (details != null && !details.isBlank())
                    ? "NumberFormatException: Failed to parse input string " + details.trim() + " into a valid number."
                    : "NumberFormatException: Failed to parse input string into a valid numeric type.";
            return MistakeAnalysis.runtimeError(MistakeType.NUMBER_FORMAT, message, combinedError.trim());
        }

        // 4. ArithmeticException
        Matcher arithMatcher = ARITHMETIC_PATTERN.matcher(combinedError);
        if (arithMatcher.find()) {
            String details = arithMatcher.group(1);
            String message = (details != null && details.contains("zero"))
                    ? "ArithmeticException: Division or modulo by zero."
                    : "ArithmeticException: Illegal arithmetic calculation.";
            return MistakeAnalysis.runtimeError(MistakeType.ARITHMETIC_EXCEPTION, message, combinedError.trim());
        }

        // 5. StackOverflowError
        if (STACK_OVERFLOW_PATTERN.matcher(combinedError).find()) {
            return MistakeAnalysis.runtimeError(
                    MistakeType.STACK_OVERFLOW,
                    "StackOverflowError: Uncontrolled or deep recursion exceeded JVM stack depth.",
                    combinedError.trim()
            );
        }

        // Generic Runtime Error
        String firstLine = combinedError.lines().filter(l -> !l.isBlank()).findFirst().orElse("Runtime exception occurred");
        return MistakeAnalysis.runtimeError(
                MistakeType.RUNTIME_ERROR,
                "Runtime error: " + firstLine,
                combinedError.trim()
        );
    }

    private MistakeAnalysis analyzeWrongAnswer(SubmissionEvaluationResult results) {
        if (results.testCaseResults() == null || results.testCaseResults().isEmpty()) {
            return MistakeAnalysis.wrongAnswer("Output did not match expected solution.", "");
        }

        // Find first failed test case
        for (TestCaseEvaluationResult tc : results.testCaseResults()) {
            if (!tc.passed() && tc.verdict() == EvaluationVerdict.WRONG_ANSWER) {
                if (!tc.isHidden()) {
                    String exp = tc.expectedOutput() != null ? tc.expectedOutput().trim() : "";
                    String act = tc.actualOutput() != null ? tc.actualOutput().trim() : "";
                    String message = "Output mismatch on test case #" + tc.testNumber() + ": Expected [" + exp + "] but got [" + act + "].";
                    String evidence = "Expected: " + exp + "\nActual: " + act;
                    return MistakeAnalysis.wrongAnswer(message, evidence);
                } else {
                    // Strictly mask hidden test contents
                    String message = "Output mismatch on hidden test case #" + tc.testNumber() + ".";
                    String evidence = "Hidden test assertion failed.";
                    return MistakeAnalysis.wrongAnswer(message, evidence);
                }
            }
        }

        return MistakeAnalysis.wrongAnswer("Output did not match expected test results.", "");
    }

    private String collectRuntimeErrorOutput(SubmissionEvaluationResult results) {
        if (results.testCaseResults() != null) {
            for (TestCaseEvaluationResult tc : results.testCaseResults()) {
                if (tc.errorMessage() != null && !tc.errorMessage().isBlank()) {
                    return tc.errorMessage();
                }
            }
        }
        return results.compilationError() != null ? results.compilationError() : "";
    }

    private String extractStackLocation(String errorText) {
        Matcher matcher = STACK_TRACE_LOCATION_PATTERN.matcher(errorText);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
