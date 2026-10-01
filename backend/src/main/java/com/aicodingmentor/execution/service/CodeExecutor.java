package com.aicodingmentor.execution.service;

import com.aicodingmentor.execution.model.ExecutionRequest;
import com.aicodingmentor.execution.model.ExecutionResult;

public interface CodeExecutor {

    /**
     * Executes untrusted user-submitted Java source code inside an isolated sandbox environment.
     * User code is NEVER executed inside the Spring Boot JVM process.
     *
     * @param request the execution request containing source code, limits, and optional stdin
     * @return the execution result containing success status, stdout, stderr, exit code, and metrics
     */
    ExecutionResult execute(ExecutionRequest request);
}
