package com.aicodingmentor.execution.service;

import com.aicodingmentor.execution.model.ExecutionFailureReason;
import com.aicodingmentor.execution.model.ExecutionRequest;
import com.aicodingmentor.execution.model.ExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DockerJavaExecutor Unit & Integration Tests")
class DockerJavaExecutorTest {

    private DockerJavaExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new DockerJavaExecutor();
    }

    @Test
    @DisplayName("Should successfully compile and execute a valid Java program")
    void testExecute_validProgram_success() {
        String source = """
                public class Solution {
                    public static void main(String[] args) {
                        System.out.println("Hello, Sandbox!");
                    }
                }
                """;

        ExecutionRequest request = ExecutionRequest.of(source);
        ExecutionResult result = executor.execute(request);

        assertThat(result.success()).isTrue();
        assertThat(result.stdout().trim()).isEqualTo("Hello, Sandbox!");
        assertThat(result.exitCode()).isZero();
        assertThat(result.executionTimeMs()).isPositive();
        assertThat(result.failureReason()).isNull();
    }

    @Test
    @DisplayName("Should successfully pass standard input to the running program")
    void testExecute_validProgramWithStdin_success() {
        String source = """
                import java.util.Scanner;
                public class Calculator {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int a = sc.nextInt();
                        int b = sc.nextInt();
                        System.out.println("SUM=" + (a + b));
                    }
                }
                """;

        ExecutionRequest request = ExecutionRequest.of(source, "15 27\n");
        ExecutionResult result = executor.execute(request);

        assertThat(result.success()).isTrue();
        assertThat(result.stdout().trim()).isEqualTo("SUM=42");
        assertThat(result.exitCode()).isZero();
    }

    @Test
    @DisplayName("Should capture compilation failure with error details in stderr")
    void testExecute_compilationFailure_returnsCompilationError() {
        String source = """
                public class BrokenSyntax {
                    public static void main(String[] args) {
                        int x = ; // syntax error
                    }
                }
                """;

        ExecutionRequest request = ExecutionRequest.of(source);
        ExecutionResult result = executor.execute(request);

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).isEqualTo(ExecutionFailureReason.COMPILATION_ERROR);
        assertThat(result.stderr()).isNotEmpty();
        assertThat(result.exitCode()).isNotZero();
    }

    @Test
    @DisplayName("Should capture runtime exception and return runtime error")
    void testExecute_runtimeException_returnsRuntimeError() {
        String source = """
                public class CrashProgram {
                    public static void main(String[] args) {
                        int a = 10;
                        int b = 0;
                        System.out.println(a / b);
                    }
                }
                """;

        ExecutionRequest request = ExecutionRequest.of(source);
        ExecutionResult result = executor.execute(request);

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).isEqualTo(ExecutionFailureReason.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("ArithmeticException");
        assertThat(result.exitCode()).isNotZero();
    }

    @Test
    @DisplayName("Should enforce timeout and kill infinite loop")
    void testExecute_infiniteLoop_timesOut() {
        String source = """
                public class InfiniteLoop {
                    public static void main(String[] args) {
                        while (true) {
                            // Busy loop
                        }
                    }
                }
                """;

        ExecutionRequest request = ExecutionRequest.of(source, "", 1500L);
        ExecutionResult result = executor.execute(request);

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).isEqualTo(ExecutionFailureReason.TIME_LIMIT_EXCEEDED);
        assertThat(result.executionTimeMs()).isGreaterThanOrEqualTo(1400L);
    }

    @Test
    @DisplayName("Should handle excessive log bomb / large output by bounding streams")
    void testExecute_largeOutput_exceedsOutputLimit() {
        String source = """
                public class OutputSpam {
                    public static void main(String[] args) {
                        for (int i = 0; i < 50000; i++) {
                            System.out.println("Line " + i + ": AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                        }
                    }
                }
                """;

        ExecutionRequest request = new ExecutionRequest(
                source,
                "OutputSpam",
                "",
                5000L,
                256,
                1.0,
                512 // Cap at 512 bytes
        );

        ExecutionResult result = executor.execute(request);

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).isEqualTo(ExecutionFailureReason.OUTPUT_LIMIT_EXCEEDED);
        assertThat(result.stdout().length()).isLessThanOrEqualTo(1024);
    }

    @Test
    @DisplayName("Should reject invalid or empty source code")
    void testExecute_invalidSource_returnsInvalidSource() {
        ExecutionResult emptyResult = executor.execute(ExecutionRequest.of("   "));
        assertThat(emptyResult.success()).isFalse();
        assertThat(emptyResult.failureReason()).isEqualTo(ExecutionFailureReason.INVALID_SOURCE);

        ExecutionResult nullByteResult = executor.execute(ExecutionRequest.of("class Test { \0 }"));
        assertThat(nullByteResult.success()).isFalse();
        assertThat(nullByteResult.failureReason()).isEqualTo(ExecutionFailureReason.INVALID_SOURCE);
    }

    @Test
    @DisplayName("Should ensure workspace is cleaned up after execution finishes")
    void testExecute_workspaceCleanup_guaranteed() throws IOException {
        String source = """
                public class CleanTest {
                    public static void main(String[] args) {
                        System.out.println("Clean check");
                    }
                }
                """;

        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
        long beforeCount;
        try (Stream<Path> stream = Files.list(tempDir)) {
            beforeCount = stream.filter(p -> p.getFileName().toString().startsWith("sandbox-exec-")).count();
        }

        ExecutionResult result = executor.execute(ExecutionRequest.of(source));
        assertThat(result.success()).isTrue();

        long afterCount;
        try (Stream<Path> stream = Files.list(tempDir)) {
            afterCount = stream.filter(p -> p.getFileName().toString().startsWith("sandbox-exec-")).count();
        }

        assertThat(afterCount).isEqualTo(beforeCount);
    }
}
