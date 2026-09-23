# Security Policy & Architecture

## 1. Core Security Principles

Security is a first-class requirement across all layers of the **AI Coding Mentor** application:
- **Defense in Depth**: Multiple security layers (Network isolation, Docker sandbox limits, Controller validation, JWT authorization, sanitized logging).
- **Least Privilege**: Application containers, database users, and sandbox execution run with the minimal required permissions.
- **Fail Secure**: When unexpected failures occur, the system shuts down or returns generic errors without exposing internal traces or system details.
- **Zero Secrets in Repository**: No passwords, private keys, or API tokens are checked into Git.

---

## 2. Authentication & Authorization

- **Password Hashing**: Passwords stored using Spring Security's `BCryptPasswordEncoder` with work factor $\ge 12$.
- **Stateless JWTs**:
  - Tokens signed with HMAC-SHA256 (or RS256 for public verification) using keys of at least 256 bits.
  - Short-lived access tokens (e.g. 15-60 minutes) and refresh token mechanisms.
- **Role-Based Access Control (RBAC)**:
  - `ROLE_LEARNER`: Can view active problems, submit code, request hints, view personal progress.
  - `ROLE_ADMIN`: Can create/edit problems, view all submission logs, configure system thresholds.

---

## 3. Data Protection & Hidden Test Confidentiality

- **Confidential Test Suites**:
  - `test_cases` marked with `is_hidden = true` evaluate corner cases, scale thresholds, and edge inputs.
  - Public problem endpoints NEVER serialize `is_hidden = true` records.
  - Submission evaluation endpoints return only the count of passed tests (e.g., `5/8 tests passed`), preventing learners from reverse-engineering test inputs via automated scraping.
- **No Secret Leaks**:
  - Sensitive environment variables are injected exclusively through `.env` or container orchestrators.
  - `.gitignore` prevents inadvertent commits of local environment configurations.

---

## 4. Input Validation & API Hardening

- **DTO Validation**: Every incoming request body is validated using Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Size`, `@Pattern`).
- **Code Submission Limits**:
  - Maximum source code length restricted (e.g., 64 KB).
  - Malformed or binary payloads rejected at the HTTP gateway.
- **Rate Limiting**:
  - Redis-backed rate limiting on submission and hint endpoints (e.g., maximum 10 submissions per minute per user; maximum 5 hint requests per minute).
  - Protects against Denial-of-Service and excessive inference load.

---

## 5. Sandboxed Code Execution Safeguards

To prevent untrusted Java code from harming infrastructure:
1. **Network Disconnected**: Ephemeral sandbox containers run with `--network none`.
2. **Resource Constraints**:
   - Memory capped at 256 MB.
   - PIDs limited to 64 to thwart fork bombs.
   - CPU quota limited to 1 core.
   - Wall-clock timeout of 5 seconds enforced by process supervisor.
3. **Non-Root User**: Code runs under an unprivileged user (`sandbox`).
4. **No Host Execution**: Code is NEVER compiled or run on the host Spring Boot JVM.
