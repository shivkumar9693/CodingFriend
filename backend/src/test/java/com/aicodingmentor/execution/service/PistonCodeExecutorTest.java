package com.aicodingmentor.execution.service;

import com.aicodingmentor.execution.model.ExecutionFailureReason;
import com.aicodingmentor.execution.model.ExecutionRequest;
import com.aicodingmentor.execution.model.ExecutionResult;
import com.aicodingmentor.execution.model.SupportedLanguage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PistonCodeExecutorTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @Mock
    private CodeExecutor fallbackExecutor;

    private ObjectMapper objectMapper;
    private PistonCodeExecutor executor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        executor = new PistonCodeExecutor(
                "https://emkc.org/api/v2/piston/execute",
                true,
                Duration.ofSeconds(5),
                httpClient,
                fallbackExecutor,
                objectMapper
        );
    }

    @Test
    @DisplayName("Should parse successful execution result from Piston")
    void testExecute_success() throws Exception {
        String jsonPayload = """
                {
                  "language": "java",
                  "version": "15.0.2",
                  "run": {
                    "stdout": "[0, 1]\\n",
                    "stderr": "",
                    "code": 0,
                    "signal": null,
                    "output": "[0, 1]\\n"
                  }
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonPayload);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        ExecutionRequest request = ExecutionRequest.of(
                "public class Solution { public static void main(String[] args) { System.out.println(\"[0, 1]\"); } }",
                "[2,7,11,15]\n9",
                SupportedLanguage.JAVA
        );

        ExecutionResult result = executor.execute(request);

        assertThat(result.success()).isTrue();
        assertThat(result.stdout()).contains("[0, 1]");
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.failureReason()).isNull();
    }

    @Test
    @DisplayName("Should detect compilation error from Piston compile block")
    void testExecute_compilationError() throws Exception {
        String jsonPayload = """
                {
                  "language": "java",
                  "version": "15.0.2",
                  "compile": {
                    "stdout": "",
                    "stderr": "Solution.java:5: error: ';' expected\\n",
                    "code": 1,
                    "output": "Solution.java:5: error: ';' expected\\n"
                  }
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonPayload);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        ExecutionRequest request = ExecutionRequest.of("invalid java code", "", SupportedLanguage.JAVA);
        ExecutionResult result = executor.execute(request);

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).isEqualTo(ExecutionFailureReason.COMPILATION_ERROR);
        assertThat(result.stderr()).contains("error: ';' expected");
    }

    @Test
    @DisplayName("Should detect runtime error from Piston run block with non-zero exit code")
    void testExecute_runtimeError() throws Exception {
        String jsonPayload = """
                {
                  "language": "java",
                  "version": "15.0.2",
                  "run": {
                    "stdout": "",
                    "stderr": "Exception in thread \\"main\\" java.lang.ArrayIndexOutOfBoundsException: Index 5 out of bounds for length 2",
                    "code": 1,
                    "output": "Exception in thread \\"main\\" java.lang.ArrayIndexOutOfBoundsException: Index 5 out of bounds for length 2"
                  }
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonPayload);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        ExecutionRequest request = ExecutionRequest.of("code with runtime error", "", SupportedLanguage.JAVA);
        ExecutionResult result = executor.execute(request);

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).isEqualTo(ExecutionFailureReason.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("ArrayIndexOutOfBoundsException");
    }

    @Test
    @DisplayName("Should cascade to local fallback executor on network timeout")
    void testExecute_fallbackOnTimeout() throws Exception {
        doThrow(new HttpTimeoutException("Piston connection timed out"))
                .when(httpClient).send(any(HttpRequest.class), any());

        ExecutionRequest request = ExecutionRequest.of("some code", "", SupportedLanguage.JAVA);
        ExecutionResult expectedFallback = ExecutionResult.success("fallback output", "", 0, 150, 1024L);
        when(fallbackExecutor.execute(request)).thenReturn(expectedFallback);

        ExecutionResult result = executor.execute(request);

        assertThat(result).isEqualTo(expectedFallback);
        verify(fallbackExecutor).execute(request);
    }

    @Test
    @DisplayName("Should execute batch of test cases efficiently")
    void testExecuteBatch() throws Exception {
        String jsonPayload = """
                {
                  "language": "java",
                  "version": "15.0.2",
                  "run": {
                    "stdout": "42\\n",
                    "stderr": "",
                    "code": 0,
                    "output": "42\\n"
                  }
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonPayload);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        ExecutionRequest baseRequest = ExecutionRequest.of("some code", "", SupportedLanguage.JAVA);
        List<ExecutionResult> results = executor.executeBatch(baseRequest, List.of("input1", "input2"));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).success()).isTrue();
        assertThat(results.get(1).success()).isTrue();
    }
}
