# Testing Strategy & Quality Assurance

## 1. Quality Standards

To maintain production excellence and long-term stability:
- **Test-Driven or Test-Accompanied**: Every new feature must be accompanied by automated tests.
- **Root Cause Resolution**: When a test fails, diagnose and fix the root cause. Never weaken or skip tests to force builds to pass.
- **No Test Degradation**: Maintain high code coverage across core domain modules.

---

## 2. Test Pyramid

```
        /  E2E Tests  \          Frontend to Backend User Flows
       / Integration   \         Testcontainers, Postgres, Redis, API Tests
      /   Unit Tests    \        Pure Java domain logic, DTO validation, Mocks
```

### 2.1 Unit Tests (Backend)
- **Framework**: JUnit 5 + Mockito + AssertJ.
- **Scope**:
  - Mistake classification logic.
  - Socratic hint tier selection.
  - LLM response JSON deserialization and anti-leak regex validation.
  - DTO validation constraints (`@Valid`).
- **Speed**: Must execute within milliseconds without opening database connections.

### 2.2 Integration Tests (Backend)
- **Framework**: Spring Boot Test (`@SpringBootTest`), MockMvc, Testcontainers.
- **Scope**:
  - API endpoint contracts and HTTP status codes.
  - Global Exception Handler mappings.
  - Flyway migration verification with real PostgreSQL container.
  - Spring Data JPA repository queries.

### 2.3 Frontend Tests
- **Framework**: Vitest + React Testing Library.
- **Scope**:
  - Editor state management and code persistence.
  - Socratic hint message rendering and progressive display.
  - API response handling and error alert states.

---

## 3. Test Execution Commands

### Backend
```bash
cd backend
# Run all unit and integration tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=GlobalExceptionHandlerTest
```

### Frontend
```bash
cd frontend
# Run TypeScript compilation and build check
npm run build
```
