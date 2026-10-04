package com.aicodingmentor.execution.service;

import com.aicodingmentor.execution.model.ExecutionFailureReason;
import com.aicodingmentor.execution.model.ExecutionRequest;
import com.aicodingmentor.execution.model.ExecutionResult;
import com.aicodingmentor.execution.model.SupportedLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("multiLanguageExecutor")
public class MultiLanguageExecutor implements CodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(MultiLanguageExecutor.class);

    private static final Pattern JAVA_CLASS_NAME_PATTERN = Pattern.compile("(?:public\\s+)?class\\s+([A-Za-z0-9_]+)");
    private static final long COMPILATION_TIMEOUT_MS = 10000L;

    private static final String DOCKER_JAVA_IMAGE = "eclipse-temurin:17-jdk-alpine";
    private static final String DOCKER_PYTHON_IMAGE = "python:3.11-alpine";
    private static final String DOCKER_CPP_IMAGE = "gcc:13-alpine";
    private static final String DOCKER_NODE_IMAGE = "node:20-alpine";

    private final boolean dockerAvailable;

    public MultiLanguageExecutor() {
        this.dockerAvailable = checkDockerAvailability();
        if (dockerAvailable) {
            log.info("Docker daemon detected. Code executor will enforce container-isolated sandboxing.");
        } else {
            log.warn("Docker daemon not detected. Code executor will use native OS subprocess sandboxing.");
        }
    }

    public boolean isDockerAvailable() {
        return dockerAvailable;
    }

    @Override
    public ExecutionResult execute(ExecutionRequest request) {
        long startTime = System.currentTimeMillis();

        if (request == null || request.sourceCode() == null || request.sourceCode().trim().isEmpty()) {
            return ExecutionResult.invalidSource("Source code must not be null or empty.");
        }

        if (request.sourceCode().contains("\0")) {
            return ExecutionResult.invalidSource("Source code contains illegal null byte characters.");
        }

        SupportedLanguage language = request.language() != null ? request.language() : SupportedLanguage.JAVA;
        Path workspace = null;

        try {
            workspace = Files.createTempDirectory("sandbox-exec-");

            return switch (language) {
                case JAVA -> executeJava(workspace, request, startTime);
                case PYTHON -> executePython(workspace, request, startTime);
                case CPP -> executeCpp(workspace, request, startTime);
                case JAVASCRIPT -> executeJavaScript(workspace, request, startTime);
            };
        } catch (Exception ex) {
            log.error("Unexpected error during multi-language sandbox execution", ex);
            return ExecutionResult.systemError("System error during execution: " + ex.getMessage(), 0);
        } finally {
            cleanupWorkspace(workspace);
        }
    }

    @Override
    public List<ExecutionResult> executeBatch(ExecutionRequest baseRequest, List<String> stdinInputs) {
        if (baseRequest == null || baseRequest.sourceCode() == null || baseRequest.sourceCode().trim().isEmpty()) {
            return List.of(ExecutionResult.invalidSource("Source code must not be null or empty."));
        }
        if (baseRequest.sourceCode().contains("\0")) {
            return List.of(ExecutionResult.invalidSource("Source code contains illegal null byte characters."));
        }
        if (stdinInputs == null || stdinInputs.isEmpty()) {
            return List.of(execute(baseRequest));
        }

        SupportedLanguage language = baseRequest.language() != null ? baseRequest.language() : SupportedLanguage.JAVA;
        Path workspace = null;
        try {
            workspace = Files.createTempDirectory("sandbox-batch-");
            return switch (language) {
                case JAVA -> executeJavaBatch(workspace, baseRequest, stdinInputs);
                case PYTHON -> executePythonBatch(workspace, baseRequest, stdinInputs);
                case CPP -> executeCppBatch(workspace, baseRequest, stdinInputs);
                case JAVASCRIPT -> executeJavaScriptBatch(workspace, baseRequest, stdinInputs);
            };
        } catch (Exception ex) {
            log.error("Unexpected error during multi-language batch sandbox execution", ex);
            return List.of(ExecutionResult.systemError("System error during batch execution: " + ex.getMessage(), 0));
        } finally {
            cleanupWorkspace(workspace);
        }
    }

    private List<ExecutionResult> executeJavaBatch(Path workspace, ExecutionRequest request, List<String> stdinInputs) throws Exception {
        String sourceCode = request.sourceCode();
        if (!sourceCode.contains("import java.util.")) {
            sourceCode = "import java.util.*;\nimport java.util.stream.*;\nimport java.io.*;\nimport java.math.*;\n" + sourceCode;
        }
        String className = resolveJavaClassName(sourceCode, request.className());
        Path sourceFile = workspace.resolve(className + ".java");
        Files.writeString(sourceFile, sourceCode, StandardCharsets.UTF_8);

        // Step 1: Compile ONCE for all test cases
        long compileStart = System.currentTimeMillis();
        CompilationResult compileResult = compileJava(workspace, sourceFile, className);
        long compileDuration = System.currentTimeMillis() - compileStart;

        if (!compileResult.success()) {
            return List.of(ExecutionResult.compilationError(compileResult.stderr(), compileResult.exitCode(), compileDuration));
        }

        // Step 2: Run pre-compiled bytecode against each input
        List<ExecutionResult> results = new ArrayList<>(stdinInputs.size());
        for (String input : stdinInputs) {
            ExecutionRequest singleReq = new ExecutionRequest(
                    request.sourceCode(),
                    request.className(),
                    input,
                    request.timeoutMs(),
                    request.memoryLimitMb(),
                    request.cpuQuota(),
                    request.maxOutputSizeBytes(),
                    request.language()
            );

            long runStart = System.currentTimeMillis();
            ProcessBuilder pb;
            if (dockerAvailable) {
                int memLimit = request.memoryLimitMb();
                int jvmHeap = Math.max(32, memLimit - 64);
                List<String> cmd = List.of(
                        "docker", "run", "--rm", "-i",
                        "--network", "none",
                        "--memory", memLimit + "m",
                        "--cpus", String.valueOf(request.cpuQuota()),
                        "-v", workspace.toAbsolutePath() + ":/workspace:ro",
                        "-w", "/workspace",
                        DOCKER_JAVA_IMAGE,
                        "java", "-Xmx" + jvmHeap + "m", "-XX:+UseSerialGC", "-Dfile.encoding=UTF-8", "-cp", "/workspace", className
                );
                pb = new ProcessBuilder(cmd);
            } else {
                String javaPath = getJdkToolPath("java");
                int jvmHeap = Math.max(32, request.memoryLimitMb() - 32);
                List<String> cmd = List.of(
                        javaPath,
                        "-Xmx" + jvmHeap + "m",
                        "-XX:+UseSerialGC",
                        "-Dfile.encoding=UTF-8",
                        "-cp", workspace.toAbsolutePath().toString(),
                        className
                );
                pb = new ProcessBuilder(cmd);
                pb.directory(workspace.toFile());
                sanitizeEnvironment(pb);
            }
            results.add(runProcess(pb, singleReq, runStart));
        }
        return results;
    }

    private List<ExecutionResult> executePythonBatch(Path workspace, ExecutionRequest request, List<String> stdinInputs) throws Exception {
        Path scriptFile = workspace.resolve("solution.py");
        Files.writeString(scriptFile, request.sourceCode(), StandardCharsets.UTF_8);

        List<ExecutionResult> results = new ArrayList<>(stdinInputs.size());
        for (String input : stdinInputs) {
            ExecutionRequest singleReq = new ExecutionRequest(
                    request.sourceCode(),
                    request.className(),
                    input,
                    request.timeoutMs(),
                    request.memoryLimitMb(),
                    request.cpuQuota(),
                    request.maxOutputSizeBytes(),
                    request.language()
            );

            long runStart = System.currentTimeMillis();
            ProcessBuilder pb;
            if (dockerAvailable) {
                List<String> cmd = List.of(
                        "docker", "run", "--rm", "-i",
                        "--network", "none",
                        "--memory", request.memoryLimitMb() + "m",
                        "--cpus", String.valueOf(request.cpuQuota()),
                        "-v", workspace.toAbsolutePath() + ":/workspace:ro",
                        "-w", "/workspace",
                        DOCKER_PYTHON_IMAGE,
                        "python", "-u", "solution.py"
                );
                pb = new ProcessBuilder(cmd);
            } else {
                String pythonCmd = findPythonExecutable();
                List<String> cmd = List.of(pythonCmd, "-u", scriptFile.toAbsolutePath().toString());
                pb = new ProcessBuilder(cmd);
                pb.directory(workspace.toFile());
                sanitizeEnvironment(pb);
            }
            results.add(runProcess(pb, singleReq, runStart));
        }
        return results;
    }

    private List<ExecutionResult> executeCppBatch(Path workspace, ExecutionRequest request, List<String> stdinInputs) throws Exception {
        Path sourceFile = workspace.resolve("solution.cpp");
        Files.writeString(sourceFile, request.sourceCode(), StandardCharsets.UTF_8);

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String exeName = isWindows && !dockerAvailable ? "solution.exe" : "solution";
        Path exePath = workspace.resolve(exeName);

        // Step 1: Compile ONCE for all test cases
        long compileStart = System.currentTimeMillis();
        CompilationResult compileResult = compileCpp(workspace, sourceFile, exePath);
        long compileDuration = System.currentTimeMillis() - compileStart;

        if (!compileResult.success()) {
            return List.of(ExecutionResult.compilationError(compileResult.stderr(), compileResult.exitCode(), compileDuration));
        }

        // Step 2: Run compiled executable against each input
        List<ExecutionResult> results = new ArrayList<>(stdinInputs.size());
        for (String input : stdinInputs) {
            ExecutionRequest singleReq = new ExecutionRequest(
                    request.sourceCode(),
                    request.className(),
                    input,
                    request.timeoutMs(),
                    request.memoryLimitMb(),
                    request.cpuQuota(),
                    request.maxOutputSizeBytes(),
                    request.language()
            );

            long runStart = System.currentTimeMillis();
            ProcessBuilder pb;
            if (dockerAvailable) {
                List<String> cmd = List.of(
                        "docker", "run", "--rm", "-i",
                        "--network", "none",
                        "--memory", request.memoryLimitMb() + "m",
                        "--cpus", String.valueOf(request.cpuQuota()),
                        "-v", workspace.toAbsolutePath() + ":/workspace:ro",
                        "-w", "/workspace",
                        DOCKER_CPP_IMAGE,
                        "./solution"
                );
                pb = new ProcessBuilder(cmd);
            } else {
                List<String> cmd = List.of(exePath.toAbsolutePath().toString());
                pb = new ProcessBuilder(cmd);
                pb.directory(workspace.toFile());
                sanitizeEnvironment(pb);
            }
            results.add(runProcess(pb, singleReq, runStart));
        }
        return results;
    }

    private List<ExecutionResult> executeJavaScriptBatch(Path workspace, ExecutionRequest request, List<String> stdinInputs) throws Exception {
        Path scriptFile = workspace.resolve("solution.js");
        Files.writeString(scriptFile, request.sourceCode(), StandardCharsets.UTF_8);

        List<ExecutionResult> results = new ArrayList<>(stdinInputs.size());
        for (String input : stdinInputs) {
            ExecutionRequest singleReq = new ExecutionRequest(
                    request.sourceCode(),
                    request.className(),
                    input,
                    request.timeoutMs(),
                    request.memoryLimitMb(),
                    request.cpuQuota(),
                    request.maxOutputSizeBytes(),
                    request.language()
            );

            long runStart = System.currentTimeMillis();
            ProcessBuilder pb;
            if (dockerAvailable) {
                List<String> cmd = List.of(
                        "docker", "run", "--rm", "-i",
                        "--network", "none",
                        "--memory", request.memoryLimitMb() + "m",
                        "--cpus", String.valueOf(request.cpuQuota()),
                        "-v", workspace.toAbsolutePath() + ":/workspace:ro",
                        "-w", "/workspace",
                        DOCKER_NODE_IMAGE,
                        "node", "solution.js"
                );
                pb = new ProcessBuilder(cmd);
            } else {
                String nodeCmd = findNodeExecutable();
                List<String> cmd = List.of(nodeCmd, scriptFile.toAbsolutePath().toString());
                pb = new ProcessBuilder(cmd);
                pb.directory(workspace.toFile());
                sanitizeEnvironment(pb);
            }
            results.add(runProcess(pb, singleReq, runStart));
        }
        return results;
    }

    // ==========================================
    // JAVA EXECUTION
    // ==========================================
    private ExecutionResult executeJava(Path workspace, ExecutionRequest request, long startTime) throws Exception {
        String sourceCode = request.sourceCode();
        if (!sourceCode.contains("import java.util.")) {
            sourceCode = "import java.util.*;\nimport java.util.stream.*;\nimport java.io.*;\nimport java.math.*;\n" + sourceCode;
        }
        String className = resolveJavaClassName(sourceCode, request.className());
        Path sourceFile = workspace.resolve(className + ".java");
        Files.writeString(sourceFile, sourceCode, StandardCharsets.UTF_8);

        // Step 1: Compile Java
        long compileStart = System.currentTimeMillis();
        CompilationResult compileResult = compileJava(workspace, sourceFile, className);
        long compileDuration = System.currentTimeMillis() - compileStart;

        if (!compileResult.success()) {
            return ExecutionResult.compilationError(compileResult.stderr(), compileResult.exitCode(), compileDuration);
        }

        // Step 2: Run Java
        long runStart = System.currentTimeMillis();
        ProcessBuilder pb;
        if (dockerAvailable) {
            int memLimit = request.memoryLimitMb();
            int jvmHeap = Math.max(32, memLimit - 64);
            List<String> cmd = List.of(
                    "docker", "run", "--rm", "-i",
                    "--network", "none",
                    "--memory", memLimit + "m",
                    "--cpus", String.valueOf(request.cpuQuota()),
                    "-v", workspace.toAbsolutePath() + ":/workspace:ro",
                    "-w", "/workspace",
                    DOCKER_JAVA_IMAGE,
                    "java", "-Xmx" + jvmHeap + "m", "-XX:+UseSerialGC", "-Dfile.encoding=UTF-8", "-cp", "/workspace", className
            );
            pb = new ProcessBuilder(cmd);
        } else {
            String javaPath = getJdkToolPath("java");
            int jvmHeap = Math.max(32, request.memoryLimitMb() - 32);
            List<String> cmd = List.of(
                    javaPath,
                    "-Xmx" + jvmHeap + "m",
                    "-XX:+UseSerialGC",
                    "-Dfile.encoding=UTF-8",
                    "-cp", workspace.toAbsolutePath().toString(),
                    className
            );
            pb = new ProcessBuilder(cmd);
            pb.directory(workspace.toFile());
            sanitizeEnvironment(pb);
        }

        return runProcess(pb, request, runStart);
    }

    private CompilationResult compileJava(Path workspace, Path sourceFile, String className) throws IOException, InterruptedException {
        // Step 1: Try in-process JavaCompiler (instant <5ms compilation, immune to process timeout or missing shell PATH)
        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (compiler != null) {
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            int exitCode = compiler.run(null, stdout, stderr, "-encoding", "UTF-8", "-d", workspace.toAbsolutePath().toString(), sourceFile.toAbsolutePath().toString());
            return new CompilationResult(exitCode == 0, stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8), exitCode);
        }

        // Step 2: Fallback to subprocess if in-process compiler is unavailable
        ProcessBuilder pb;
        if (dockerAvailable) {
            List<String> cmd = List.of(
                    "docker", "run", "--rm",
                    "--network", "none",
                    "--memory", "512m",
                    "-v", workspace.toAbsolutePath() + ":/workspace:rw",
                    "-w", "/workspace",
                    DOCKER_JAVA_IMAGE,
                    "javac", "-encoding", "UTF-8", className + ".java"
            );
            pb = new ProcessBuilder(cmd);
        } else {
            String javacPath = getJdkToolPath("javac");
            List<String> cmd = List.of(javacPath, "-encoding", "UTF-8", "-d", workspace.toAbsolutePath().toString(), sourceFile.toAbsolutePath().toString());
            pb = new ProcessBuilder(cmd);
            pb.directory(workspace.toFile());
            sanitizeEnvironment(pb);
        }
        return runCompilationProcess(pb);
    }

    // ==========================================
    // PYTHON EXECUTION
    // ==========================================
    private ExecutionResult executePython(Path workspace, ExecutionRequest request, long startTime) throws Exception {
        Path scriptFile = workspace.resolve("solution.py");
        Files.writeString(scriptFile, request.sourceCode(), StandardCharsets.UTF_8);

        long runStart = System.currentTimeMillis();
        ProcessBuilder pb;
        if (dockerAvailable) {
            List<String> cmd = List.of(
                    "docker", "run", "--rm", "-i",
                    "--network", "none",
                    "--memory", request.memoryLimitMb() + "m",
                    "--cpus", String.valueOf(request.cpuQuota()),
                    "-v", workspace.toAbsolutePath() + ":/workspace:ro",
                    "-w", "/workspace",
                    DOCKER_PYTHON_IMAGE,
                    "python", "-u", "solution.py"
            );
            pb = new ProcessBuilder(cmd);
        } else {
            String pythonCmd = findPythonExecutable();
            List<String> cmd = List.of(pythonCmd, "-u", scriptFile.toAbsolutePath().toString());
            pb = new ProcessBuilder(cmd);
            pb.directory(workspace.toFile());
            sanitizeEnvironment(pb);
        }

        return runProcess(pb, request, runStart);
    }

    // ==========================================
    // C++ EXECUTION
    // ==========================================
    private ExecutionResult executeCpp(Path workspace, ExecutionRequest request, long startTime) throws Exception {
        Path sourceFile = workspace.resolve("solution.cpp");
        Files.writeString(sourceFile, request.sourceCode(), StandardCharsets.UTF_8);

        // Step 1: Compile C++
        long compileStart = System.currentTimeMillis();
        String outputBinary = isWindows() ? "solution.exe" : "solution";
        Path exePath = workspace.resolve(outputBinary);

        CompilationResult compileResult = compileCpp(workspace, sourceFile, exePath);
        long compileDuration = System.currentTimeMillis() - compileStart;
        if (!compileResult.success()) {
            return ExecutionResult.compilationError(compileResult.stderr(), compileResult.exitCode(), compileDuration);
        }

        // Step 2: Run C++ Binary
        long runStart = System.currentTimeMillis();
        ProcessBuilder pbRun;
        if (dockerAvailable) {
            List<String> cmd = List.of(
                    "docker", "run", "--rm", "-i",
                    "--network", "none",
                    "--memory", request.memoryLimitMb() + "m",
                    "--cpus", String.valueOf(request.cpuQuota()),
                    "-v", workspace.toAbsolutePath() + ":/workspace:ro",
                    "-w", "/workspace",
                    DOCKER_CPP_IMAGE,
                    "./solution"
            );
            pbRun = new ProcessBuilder(cmd);
        } else {
            List<String> cmd = List.of(exePath.toAbsolutePath().toString());
            pbRun = new ProcessBuilder(cmd);
            pbRun.directory(workspace.toFile());
            sanitizeEnvironment(pbRun);
        }

        return runProcess(pbRun, request, runStart);
    }

    // ==========================================
    // JAVASCRIPT (NODE.JS) EXECUTION
    // ==========================================
    private ExecutionResult executeJavaScript(Path workspace, ExecutionRequest request, long startTime) throws Exception {
        Path scriptFile = workspace.resolve("solution.js");
        Files.writeString(scriptFile, request.sourceCode(), StandardCharsets.UTF_8);

        long runStart = System.currentTimeMillis();
        ProcessBuilder pb;
        if (dockerAvailable) {
            List<String> cmd = List.of(
                    "docker", "run", "--rm", "-i",
                    "--network", "none",
                    "--memory", request.memoryLimitMb() + "m",
                    "--cpus", String.valueOf(request.cpuQuota()),
                    "-v", workspace.toAbsolutePath() + ":/workspace:ro",
                    "-w", "/workspace",
                    DOCKER_NODE_IMAGE,
                    "node", "solution.js"
            );
            pb = new ProcessBuilder(cmd);
        } else {
            List<String> cmd = List.of("node", scriptFile.toAbsolutePath().toString());
            pb = new ProcessBuilder(cmd);
            pb.directory(workspace.toFile());
            sanitizeEnvironment(pb);
        }

        return runProcess(pb, request, runStart);
    }

    // ==========================================
    // COMMON PROCESS RUNNERS
    // ==========================================
    private CompilationResult runCompilationProcess(ProcessBuilder pb) throws IOException, InterruptedException {
        Process process = pb.start();
        StreamCapture stdoutCapture = new StreamCapture(process.getInputStream(), 128 * 1024);
        StreamCapture stderrCapture = new StreamCapture(process.getErrorStream(), 128 * 1024);

        boolean finished = process.waitFor(COMPILATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            return new CompilationResult(false, "", "Compilation timed out after " + COMPILATION_TIMEOUT_MS + "ms", -1);
        }

        stdoutCapture.await();
        stderrCapture.await();

        int exitCode = process.exitValue();
        return new CompilationResult(exitCode == 0, stdoutCapture.getOutput(), stderrCapture.getOutput(), exitCode);
    }

    private ExecutionResult runProcess(ProcessBuilder pb, ExecutionRequest request, long runStart) throws IOException, InterruptedException {
        Process process = pb.start();

        // Feed standard input if supplied
        if (request.stdin() != null && !request.stdin().isEmpty()) {
            try (OutputStream os = process.getOutputStream()) {
                os.write(request.stdin().getBytes(StandardCharsets.UTF_8));
                os.flush();
            } catch (IOException ignored) {
            }
        } else {
            try {
                process.getOutputStream().close();
            } catch (IOException ignored) {
            }
        }

        StreamCapture stdoutCapture = new StreamCapture(process.getInputStream(), request.maxOutputSizeBytes());
        StreamCapture stderrCapture = new StreamCapture(process.getErrorStream(), request.maxOutputSizeBytes());

        boolean finished = process.waitFor(request.timeoutMs(), TimeUnit.MILLISECONDS);
        long executionDuration = System.currentTimeMillis() - runStart;

        if (!finished) {
            process.destroyForcibly();
            stdoutCapture.await();
            stderrCapture.await();
            return ExecutionResult.timeout(executionDuration);
        }

        stdoutCapture.await();
        stderrCapture.await();

        if (stdoutCapture.isExceededLimit() || stderrCapture.isExceededLimit()) {
            return ExecutionResult.outputLimitExceeded(stdoutCapture.getOutput(), executionDuration);
        }

        int exitCode = process.exitValue();
        String stdout = stdoutCapture.getOutput();
        String stderr = stderrCapture.getOutput();

        if (exitCode != 0) {
            return ExecutionResult.runtimeError(stdout, stderr, exitCode, executionDuration);
        }

        return ExecutionResult.success(stdout, stderr, exitCode, executionDuration, null);
    }

    private String resolveJavaClassName(String sourceCode, String explicitClassName) {
        if (explicitClassName != null && !explicitClassName.trim().isEmpty()) {
            return explicitClassName.trim();
        }
        if (sourceCode.contains("class MainDriver")) {
            return "MainDriver";
        }
        Matcher publicMatcher = Pattern.compile("public\\s+class\\s+([A-Za-z0-9_]+)").matcher(sourceCode);
        if (publicMatcher.find()) {
            return publicMatcher.group(1);
        }
        Matcher matcher = Pattern.compile("class\\s+([A-Za-z0-9_]+)").matcher(sourceCode);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Solution";
    }

    private String findPythonExecutable() {
        if (isWindows()) {
            // Check if 'python' works on Windows
            return "python";
        }
        return "python3";
    }

    private String findNodeExecutable() {
        if (isWindows()) {
            return "node.exe";
        }
        return "node";
    }

    private CompilationResult compileCpp(Path workspace, Path sourceFile, Path exePath) throws IOException, InterruptedException {
        ProcessBuilder pbCompile;
        if (dockerAvailable) {
            List<String> cmd = List.of(
                    "docker", "run", "--rm",
                    "--network", "none",
                    "--memory", "512m",
                    "-v", workspace.toAbsolutePath() + ":/workspace:rw",
                    "-w", "/workspace",
                    DOCKER_CPP_IMAGE,
                    "g++", "-O3", "-std=c++17", "solution.cpp", "-o", "solution"
            );
            pbCompile = new ProcessBuilder(cmd);
        } else {
            String binaryName = exePath.getFileName().toString();
            List<String> cmd = List.of("g++", "-O3", "-std=c++17", "solution.cpp", "-o", binaryName);
            pbCompile = new ProcessBuilder(cmd);
            pbCompile.directory(workspace.toFile());
            sanitizeEnvironment(pbCompile);
        }
        return runCompilationProcess(pbCompile);
    }

    private void cleanupWorkspace(Path workspace) {
        if (workspace == null || !Files.exists(workspace)) {
            return;
        }
        try {
            Files.walkFileTree(workspace, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception ex) {
            log.warn("Failed to delete ephemeral sandbox workspace directory: {}", workspace, ex);
        }
    }

    private String getJdkToolPath(String tool) {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            Path binDir = Path.of(javaHome, "bin");
            Path exe = binDir.resolve(tool + (isWindows() ? ".exe" : ""));
            if (Files.exists(exe)) {
                return exe.toAbsolutePath().toString();
            }
        }
        return tool;
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private void sanitizeEnvironment(ProcessBuilder pb) {
        var env = pb.environment();
        String path = env.get("PATH");
        String javaHome = env.get("JAVA_HOME");
        String temp = env.get("TMPDIR") != null ? env.get("TMPDIR") : env.get("TEMP");
        String systemRoot = env.get("SystemRoot") != null ? env.get("SystemRoot") : env.get("SYSTEMROOT");
        String lang = env.get("LANG");
        String lcAll = env.get("LC_ALL");

        env.clear();
        if (path != null) env.put("PATH", path);
        if (javaHome != null) env.put("JAVA_HOME", javaHome);
        if (temp != null) {
            env.put("TMPDIR", temp);
            env.put("TEMP", temp);
            env.put("TMP", temp);
        }
        if (systemRoot != null) env.put("SystemRoot", systemRoot);
        if (lang != null) env.put("LANG", lang);
        if (lcAll != null) env.put("LC_ALL", lcAll);
    }

    private boolean checkDockerAvailability() {
        try {
            Process process = new ProcessBuilder("docker", "--version").start();
            boolean finished = process.waitFor(2000, TimeUnit.MILLISECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private record CompilationResult(boolean success, String stdout, String stderr, int exitCode) {}

    private static class StreamCapture {
        private final InputStream is;
        private final int maxBytes;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final CompletableFuture<Void> future;
        private volatile boolean exceededLimit = false;

        public StreamCapture(InputStream is, int maxBytes) {
            this.is = is;
            this.maxBytes = maxBytes;
            this.future = CompletableFuture.runAsync(this::readStream);
        }

        private void readStream() {
            byte[] buf = new byte[1024];
            int total = 0;
            int read;
            try {
                while ((read = is.read(buf)) != -1) {
                    if (!exceededLimit) {
                        if (total + read > maxBytes) {
                            int remaining = maxBytes - total;
                            if (remaining > 0) {
                                buffer.write(buf, 0, remaining);
                            }
                            exceededLimit = true;
                        } else {
                            buffer.write(buf, 0, read);
                            total += read;
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }

        public void await() {
            try {
                future.get(1000, TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {
            }
        }

        public String getOutput() {
            return buffer.toString(StandardCharsets.UTF_8);
        }

        public boolean isExceededLimit() {
            return exceededLimit;
        }
    }
}
