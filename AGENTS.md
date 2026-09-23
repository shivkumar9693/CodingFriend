# AGENTS.md - AI Coding Mentor Development Guidelines

Welcome to the **AI Coding Mentor** codebase. This file serves as the definitive specification, architectural guideline, and operational rulebook for any AI agent or software engineer contributing to this repository.

---

## 1. Project Mission & Core Vision

**AI Coding Mentor** is an online coding platform where learners solve Java coding challenges while receiving progressive, Socratic hints from an AI mentor instead of immediate solutions. 

The system focuses on:
- Diagnosing syntactic, logical, runtime, and algorithmic mistakes.
- Providing tiered hints (nudge $\to$ conceptual insight $\to$ algorithmic pattern $\to$ syntax/logic pointer).
- Guaranteeing complete code isolation: untrusted learner code is never executed directly inside the core application JVM.
- Running cost-effective, self-hostable AI inference locally via Ollama without proprietary or paid API lock-in.

---

## 2. Cardinal Development Rules (Mandatory)

Agents and contributors must strictly adhere to the following 33 development rules:

1. **Current Scope Only**: Do not implement future features unless explicitly requested.
2. **Task Focus**: Work only on the current task.
3. **Inspect Before Changing**: Inspect the existing repository structure and relevant files before changing code.
4. **Preserve Working Code**: Do not overwrite working code unnecessarily.
5. **Production Quality**: Prefer clean, maintainable, production-quality code.
6. **SOLID Principles**: Follow SOLID design principles where appropriate.
7. **Clean Controllers**: Keep business logic strictly out of controllers; controllers only handle routing, validation, and response mapping.
8. **DTO Encapsulation**: Use DTOs instead of exposing JPA entities directly through APIs.
9. **Global Exception Handling**: Use global exception handling with structured, uniform error responses.
10. **Strict Validation**: Validate all external input at controller boundaries using Bean Validation (`@Valid`, `@NotNull`, etc.).
11. **Meaningful Naming**: Use clear, intentional, and domain-accurate names for all classes, methods, variables, and files.
12. **Minimal Abstractions**: Avoid premature or unnecessary abstractions; keep designs pragmatic.
13. **Prudent Dependencies**: Do not add dependencies unless they are actually required.
14. **Automated Testing**: Every new feature must include automated tests (unit, integration, or contract).
15. **Test Execution**: Run relevant tests after implementation.
16. **Root Cause Analysis**: If tests fail, investigate and fix the root cause; do not mask problems.
17. **Test Integrity**: Do not disable, skip, or weaken tests just to make a build pass.
18. **Continuous Documentation**: Update documentation whenever architecture, API, database, setup, or behavior changes.
19. **Zero Secrets**: Never commit secrets, API keys, passwords, tokens, or local credentials to Git.
20. **Environment Synchronicity**: Update `.env.example` whenever environment variables are introduced or changed.
21. **Docker Compose Parity**: The application must remain easy to run locally via `docker-compose up`.
22. **Vercel Deployable Frontend**: The frontend must remain deployable to Vercel without custom backend coupling.
23. **Vercel Independence**: Do not make the backend dependent on Vercel-specific APIs or edge runtimes.
24. **No Paid AI APIs**: Do not use paid APIs (OpenAI, Anthropic, Gemini API keys) or external paid AI services.
25. **Local Ollama AI**: AI must be designed around a locally hosted LLM through Ollama during development.
26. **Java First**: The first supported programming language is Java only.
27. **Zero Host Execution**: Never execute learner code directly inside the Spring Boot JVM.
28. **Isolated Worker Execution**: Code execution must eventually occur inside an isolated Docker-based execution worker with strict limits (CPU, memory, timeout, disabled network, non-root).
29. **Hidden Test Privacy**: Never expose hidden test cases, test code, or test parameters through public or client APIs.
30. **Validate AI Responses**: Never trust AI output blindly; validate and parse structured AI responses before persisting or presenting them.
31. **Security First**: Treat security as a first-class requirement across all layers.
32. **Honest Completion**: Do not claim a feature is implemented unless it actually works and has been verified.
33. **Structured Task Summary**: At the end of every task, provide:
    - Files changed
    - Features implemented
    - Tests added
    - Tests executed
    - Test results
    - Documentation updated
    - Known limitations
    - Manual verification steps

---

## 3. Technology Stack

| Layer | Technology |
|---|---|
| **Backend Language** | Java 17 |
| **Backend Framework** | Spring Boot 3.x (Spring MVC, Spring Data JPA, Spring Security, Spring Validation, Actuator) |
| **Persistence** | PostgreSQL 16+ with Flyway schema migrations |
| **Caching & Queue** | Redis (caching, rate limiting) & Apache Kafka (asynchronous submission execution pipeline) |
| **API Documentation** | Springdoc OpenAPI (Swagger UI) |
| **Testing** | JUnit 5, Mockito, Testcontainers |
| **Frontend Framework** | React 18/19 with TypeScript and Vite |
| **Frontend Editor** | Monaco Editor |
| **Frontend Routing** | React Router |
| **AI Engine** | Ollama (local open-weight models such as `qwen2.5-coder`, `deepseek-coder`, or `llama3`) |
| **Containerization** | Docker, Docker Compose |
| **CI / CD** | GitHub Actions, Vercel (Frontend) |

---

## 4. Architecture & Module Boundaries

The backend is organized as a **Modular Monolith** within the package `com.aicodingmentor`. Microservices are strictly avoided in early stages, with module boundaries designed for clean separation:

```
com.aicodingmentor/
├── common/        # Shared kernel: Base entities, API wrappers, error models, utilities
├── auth/          # Authentication & authorization (JWT, credentials, sessions)
├── user/          # User accounts, profiles, learner stats
├── problem/       # Coding problems, starter code, test suites (public vs hidden)
├── submission/    # Code submission lifecycle and state tracking
├── execution/     # Dispatcher and coordinator for isolated code execution
├── mistake/       # Error classification engine (syntax, logic, runtime, timeout)
├── mentor/        # Socratic hint generator, Ollama LLM integration, prompt templates
└── learning/      # Learner mastery tracking, spaced repetition, hint history
```

### Dependency Rules Across Modules:
- **`common`** has no dependencies on other application modules.
- High-level domain modules communicate through defined service interfaces or internal domain events, never by direct cross-module entity joins.
- Controller endpoints must only access services in their own module or explicit façade services.

---

## 5. Security & Isolation Guidelines

1. **Sandboxed Code Execution**:
   - Learner code must run in an ephemeral container or dedicated sandbox with:
     - No network access (`--network none`)
     - Memory caps (e.g., 256MB)
     - CPU quota (e.g., 1 core, 50% quota)
     - Wall-clock timeout (e.g., 5 seconds)
     - Read-only root filesystem where possible
     - Non-root user permissions
2. **Hidden Test Cases**:
   - Problems define `is_hidden` test cases used solely for grading.
   - Learner-facing APIs must only return public sample test cases and execution summary (passed/total), never the hidden input/output values.
3. **AI Socratic Constraints**:
   - The AI mentor must never produce complete solutions or copy-paste code replacements.
   - System prompts must enforce progressive hint levels:
     - Level 1: Gentle Nudge (Ask an observational question)
     - Level 2: Conceptual Hint (Identify underlying programming concept or edge condition)
     - Level 3: Algorithmic Hint (Guide problem-solving structure)
     - Level 4: Targeted Pointer (Identify line or construct with the flaw without writing code)

---

## 6. End-of-Task Reporting Protocol

Upon finishing any task, the agent must output a structured summary formatted as follows:

```markdown
### Task Completion Report

- **Files Changed**: [List of added, modified, or deleted files]
- **Features Implemented**: [Specific functionality added]
- **Tests Added**: [New automated test classes or cases]
- **Tests Executed**: [Commands and test suites executed]
- **Test Results**: [Pass/fail counts, execution outcomes]
- **Documentation Updated**: [Documentation files updated]
- **Known Limitations**: [Deliberate omissions or current constraints]
- **Manual Verification Steps**: [Step-by-step instructions for human verification]
```
