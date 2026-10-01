package com.aicodingmentor.execution.service;

import com.aicodingmentor.execution.model.ExecutionFailureReason;
import com.aicodingmentor.execution.model.ExecutionRequest;
import com.aicodingmentor.execution.model.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DockerJavaExecutor implements CodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(DockerJavaExecutor.class);

    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("(?:public\\s+)?class\\s+([A-Za-z0-9_]+)");
    private static final long COMPILATION_TIMEOUT_MS = 10000L;
    private static final String DOCKER_IMAGE = "eclipse-temurin:17-jdk-alpine";

    private final boolean dockerAvailable;

    public DockerJavaExecutor() {
        this.dockerAvailable = checkDockerAvailability();
        if (dockerAvailable) {
            log.info("Docker daemon detected. Code executor will enforce Docker-isolated sandboxing.");
        } else {
            log.warn("Docker daemon not detected. Code executor will use isolated OS subprocess sandboxing.");
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

        String className = resolveClassName(request.sourceCode(), request.className());
        Path workspace = null;

        try {
            workspace = Files.createTempDirectory("sandbox-exec-");
            Path sourceFile = workspace.resolve(className + ".java");
            Files.writeString(sourceFile, request.sourceCode(), StandardCharsets.UTF_8);

            // Step 1: Compile Java Source
            long compileStart = System.currentTimeMillis();
            CompilationResult compileResult = compileJavaSource(workspace, sourceFile, className);
            long compileDuration = System.currentTimeMillis() - compileStart;

            if (!compileResult.success()) {
                return ExecutionResult.compilationError(
                        compileResult.stderr(),
                        compileResult.exitCode(),
                        compileDuration
                );
            }

            // Step 2: Execute Compiled Bytecode
            long runStart = System.currentTimeMillis();
            ExecutionResult runResult = runCompiledProgram(workspace, className, request);
            long totalDuration = System.currentTimeMillis() - startTime;

            log.debug("Execution completed for class [{}] in {}ms (success: {})", className, totalDuration, runResult.success());
            return runResult;

        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Unexpected error during sandbox execution", ex);
            return ExecutionResult.systemError("System error during execution: " + ex.getMessage(), elapsed);
        } finally {
            cleanupWorkspace(workspace);
        }
    }

    private String resolveClassName(String sourceCode, String explicitClassName) {
        if (explicitClassName != null && !explicitClassName.trim().isEmpty()) {
            return explicitClassName.trim();
        }
        Matcher matcher = CLASS_NAME_PATTERN.matcher(sourceCode);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Solution";
    }

    private CompilationResult compileJavaSource(Path workspace, Path sourceFile, String className) throws IOException, InterruptedException {
        ProcessBuilder pb;
        if (dockerAvailable) {
            List<String> cmd = List.of(
                    "docker", "run", "--rm",
                    "--network", "none",
                    "--memory", "512m",
                    "-v", workspace.toAbsolutePath() + ":/workspace:rw",
                    "-w", "/workspace",
                    DOCKER_IMAGE,
                    "javac", "-encoding", "UTF-8", className + ".java"
            );
            pb = new ProcessBuilder(cmd);
        } else {
            String javacPath = getJdkToolPath("javac");
            List<String> cmd = List.of(javacPath, "-encoding", "UTF-8", "-d", workspace.toAbsolutePath().toString(), sourceFile.toAbsolutePath().toString());
            pb = new ProcessBuilder(cmd);
            pb.directory(workspace.toFile());
            pb.environment().clear();
        }

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
        boolean success = (exitCode == 0);
        return new CompilationResult(success, stdoutCapture.getOutput(), stderrCapture.getOutput(), exitCode);
    }

    private ExecutionResult runCompiledProgram(Path workspace, String className, ExecutionRequest request) throws IOException, InterruptedException {
        long runStart = System.currentTimeMillis();
        ProcessBuilder pb;

        if (dockerAvailable) {
            int memLimit = request.memoryLimitMb();
            int jvmHeap = Math.max(32, memLimit - 64);
            List<String> cmd = new ArrayList<>(List.of(
                    "docker", "run", "--rm", "-i",
                    "--network", "none",
                    "--memory", memLimit + "m",
                    "--memory-swap", memLimit + "m",
                    "--cpus", String.valueOf(request.cpuQuota()),
                    "--pids-limit", "64",
                    "--read-only",
                    "--tmpfs", "/tmp:rw,noexec,nosuid,size=32m",
                    "-u", "1001:1001",
                    "-v", workspace.toAbsolutePath() + ":/workspace:ro",
                    "-w", "/workspace",
                    DOCKER_IMAGE,
                    "java", "-Xmx" + jvmHeap + "m", "-XX:+UseSerialGC", "-Dfile.encoding=UTF-8", "-cp", "/workspace", className
            ));
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
            pb.environment().clear();
        }

        Process process = pb.start();

        // Feed standard input if supplied
        if (request.stdin() != null && !request.stdin().isEmpty()) {
            try (OutputStream os = process.getOutputStream()) {
                os.write(request.stdin().getBytes(StandardCharsets.UTF_8));
                os.flush();
            } catch (IOException ignored) {
                // Process may have exited early before stdin read
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
            if (stdoutCapture.isExceededLimit() || stderrCapture.isExceededLimit()) {
                return ExecutionResult.outputLimitExceeded(stdoutCapture.getOutput(), executionDuration);
            }
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

    /**
     * Asynchronous bounded stream reader to prevent memory exhaustion and log bombs.
     */
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
                    // Continue discarding remainder to prevent child process deadlock on filled pipe
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
