# Requirements Specification

## 1. Executive Summary

**AI Coding Mentor** bridges the gap between passive code execution (e.g., LeetCode, HackerRank) and personalized 1-on-1 human mentorship. When a learner makes an error, traditional platforms output raw stack traces or boolean pass/fail indicators, leaving the learner stuck or tempted to view the full answer. AI Coding Mentor introduces an intelligent, Socratic tutor that analyzes code and test failures to provide guided, progressive hints without spoiling the solution.

---

## 2. Functional Requirements (FR)

### FR1: Authentication & User Accounts
- Learners can register, authenticate via email/password, and receive secure JWT access tokens.
- Role-based authorization: `LEARNER`, `ADMIN`.

### FR2: Problem Discovery & Workspace
- Learners can browse coding challenges filtered by difficulty (`EASY`, `MEDIUM`, `HARD`), topic (`Arrays`, `Strings`, `Recursion`, `OOP`), and status (`Unsolved`, `Attempted`, `Solved`).
- Problem workspace provides:
  - Problem description with markdown formatting.
  - Constraints, time limits, memory limits.
  - Visible sample test cases with expected outputs.
  - Language-specific starter code template (Java).
  - Web-based Monaco code editor.

### FR3: Submission & Execution Lifecycle
- Learners can submit Java code for evaluation.
- The system checks code against visible sample test cases and hidden evaluation test suites.
- Real-time submission status reporting (`SUBMITTED`, `COMPILING`, `RUNNING`, `ACCEPTED`, `WRONG_ANSWER`, `COMPILE_ERROR`, `TIME_LIMIT_EXCEEDED`, `RUNTIME_ERROR`).

### FR4: Mistake Analysis Engine
- When a submission fails:
  - Extract failure type: Syntax, Runtime Exception (NPE, ArrayIndexOutOfBounds), Test Assertion Failure, or Timeout.
  - Capture sanitized failure metadata (e.g., failed input signature for visible tests; generalized failure category for hidden tests).
  - Prepare mistake payload for the mentor engine.

### FR5: Progressive Socratic AI Mentor
- When a learner requests assistance or fails a problem:
  - The AI mentor provides progressive hints across 4 defined levels:
    - **Level 1 (Nudge)**: Direct attention to an unnoticed nuance or assumption.
    - **Level 2 (Conceptual Insight)**: Explain the relevant underlying principle without code.
    - **Level 3 (Algorithmic Clue)**: Suggest structural approach or pseudocode outline.
    - **Level 4 (Targeted Syntax/Logic Pointer)**: Highlight the exact erroneous line or logic block without rewriting it.
  - Strict anti-spoiler filter: The AI is prohibited from outputting working Java code solutions.
  - Responses must be structured and validated against a strict JSON schema before display.

### FR6: Privacy & Anti-Cheat
- Hidden test cases and their specific inputs/outputs must NEVER be transferred to the browser client or exposed in API payloads.

---

## 3. Non-Functional Requirements (NFR)

### NFR1: Security & Sandboxing
- **Untrusted Code Execution**: All learner Java code runs in an isolated ephemeral sandbox.
  - Host filesystem write access disabled.
  - Network interfaces disabled (`--network none`).
  - Strict resource constraints (max 256MB RAM, max 1 CPU core, max 5-second wall clock timeout).
  - Process run as non-root unprivileged user.

### NFR2: Performance & Scalability
- Submission intake latency: $< 100\text{ ms}$ (HTTP response acknowledging submission ID).
- Total evaluation turnaround: $< 5\text{ seconds}$ under standard load.
- Socratic hint generation: $< 4\text{ seconds}$ using local Ollama model.

### NFR3: Reliability & Data Integrity
- Atomic database migrations via Flyway.
- Resilient asynchronous processing with retry policies for code execution.

### NFR4: Cost & Self-Sufficiency
- Zero reliance on paid commercial LLM APIs (OpenAI, Anthropic).
- Designed to run out-of-the-box locally with Docker Compose and local Ollama inference.

### NFR5: Extensibility
- Clean modular monolith boundaries allowing future migration of execution workers or AI services to independent microservices if traffic demands.
