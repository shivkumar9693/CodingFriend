# Contributing to AI Coding Mentor

Thank you for contributing to **AI Coding Mentor**! This document outlines our development workflows, quality standards, and contribution processes.

---

## 1. Code of Conduct & Development Rules

All contributors and AI agents must abide by the rules stated in [AGENTS.md](AGENTS.md), including:
- Write clean, maintainable, and well-tested code.
- Never execute learner code inside the main application JVM.
- Never leak hidden test cases through APIs.
- Keep business logic strictly out of Spring controllers.
- Use DTOs instead of exposing JPA entities.
- Ensure all tests pass before opening a PR.

---

## 2. Getting Started

1. Fork the repository and clone it locally.
2. Ensure you have the required prerequisites:
   - Java 17+ JDK
   - Node.js 18+ and npm
   - Docker and Docker Compose
3. Copy `.env.example` to `.env` and configure local variables if necessary.
4. Run `docker-compose up -d postgres redis` to spin up supporting services.

---

## 3. Branching & Commit Conventions

- **Branch Naming**:
  - `feat/feature-name` for new capabilities
  - `fix/bug-description` for bug fixes
  - `docs/documentation-topic` for docs
  - `refactor/component-name` for structural refactoring
- **Commit Messages**: Follow Conventional Commits:
  - `feat(problem): add starter code template endpoint`
  - `fix(execution): handle timeout exit codes properly`
  - `docs(ai-mentor): specify Socratic prompt structure`
  - `test(auth): add integration test for JWT refresh`

---

## 4. Code Standards

### Backend (Java / Spring Boot)
- Format code according to Google Java Style conventions.
- Package domain features into modular monolith structures under `com.aicodingmentor.<module>`.
- Use Bean Validation (`@NotNull`, `@Size`, `@Pattern`) for DTO fields.
- Write unit tests with JUnit 5 and Mockito, and integration tests using Testcontainers.

### Frontend (React / TypeScript)
- Use functional components with TypeScript interfaces for props and states.
- Avoid external UI component libraries when simple, accessible custom components or standard CSS suffice.
- Keep components modular, reusable, and free of hardcoded API hostnames.

---

## 5. Pull Request Checklist

Before submitting a Pull Request, ensure:
- [ ] Code builds cleanly without warnings or errors.
- [ ] All automated tests pass (`./mvnw test` and `npm run test` / `npm run build`).
- [ ] No secrets, passwords, or credentials are committed.
- [ ] Documentation is updated under `docs/` if architectural, API, or configuration changes occurred.
- [ ] `.env.example` is updated if new environment variables were introduced.
