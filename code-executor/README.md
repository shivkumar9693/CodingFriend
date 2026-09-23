# Code Executor Sandbox

This module defines the isolated sandbox container and execution runner for executing untrusted learner Java code.

## Key Invariants
- **Isolated Execution**: User code is NEVER executed inside the Spring Boot JVM.
- **Resource Restricted**: Ephemeral containers launch with:
  - `--network none`
  - `--memory 256m`
  - `--cpus 1.0`
  - Wall-clock timeout of 5 seconds
- **Non-Root**: Operates under unprivileged `sandbox` user (UID 1001).
- **Sanitized Results**: Returns a structured JSON execution report containing pass/fail states, elapsed time, memory usage, and failure diagnostics.
