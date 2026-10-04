package com.aicodingmentor.execution.service;

import com.aicodingmentor.execution.model.ExecutionRequest;
import com.aicodingmentor.execution.model.ExecutionResult;
import java.util.List;

public interface CodeExecutor {

    /**
     * Executes untrusted user-submitted Java source code inside an isolated sandbox environment.
     * User code is NEVER executed inside the Spring Boot JVM process.
     *
     * @param request the execution request containing source code, limits, and optional stdin
     * @return the execution result containing success status, stdout, stderr, exit code, and metrics
     */
    ExecutionResult execute(ExecutionRequest request);

    /**
     * Executes a batch of test inputs against pre-compiled user code in a single sandbox lifecycle.
     * Dramatically reduces execution latency by compiling once and evaluating many inputs.
     *
     * @param baseRequest the execution request with source code and limits
     * @param stdinInputs the list of stdin input strings to execute against
     * @return list of execution results corresponding to each input
     */
    default List<ExecutionResult> executeBatch(ExecutionRequest baseRequest, List<String> stdinInputs) {
        if (stdinInputs == null || stdinInputs.isEmpty()) {
            return List.of(execute(baseRequest));
        }
        return stdinInputs.stream()
                .map(input -> execute(new ExecutionRequest(
                        baseRequest.sourceCode(),
                        baseRequest.className(),
                        input,
                        baseRequest.timeoutMs(),
                        baseRequest.memoryLimitMb(),
                        baseRequest.cpuQuota(),
                        baseRequest.maxOutputSizeBytes(),
                        baseRequest.language()
                )))
                .toList();
    }
}
