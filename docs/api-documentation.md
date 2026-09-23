# REST API Documentation & Contracts

## 1. General API Principles

- **Base URL**: `/api/v1`
- **Payload Format**: `application/json;charset=UTF-8`
- **Authentication**: Bearer JWT token in the `Authorization` header (`Bearer <token>`).
- **Standard Envelope**: All successful responses are wrapped in a uniform `ApiResponse<T>` envelope.
- **Error Envelope**: All errors follow a standard `ErrorResponse` model with HTTP status codes.
- **No Entity Leaks**: APIs exclusively consume and produce strongly-typed DTOs.
- **Hidden Tests Masking**: Test case responses for problem endpoints strictly filter out `is_hidden = true` cases.

---

## 2. Standard Response Schemas

### 2.1 Success Envelope (`ApiResponse<T>`)
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "timestamp": "2026-09-23T12:00:00Z"
}
```

### 2.2 Error Envelope (`ErrorResponse`)
```json
{
  "success": false,
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid submission payload",
  "timestamp": "2026-09-23T12:00:00Z",
  "validationErrors": [
    {
      "field": "sourceCode",
      "message": "Source code cannot be blank"
    }
  ]
}
```

---

## 3. Core Endpoint Specifications

### 3.1 Authentication (`/api/v1/auth`)

#### `POST /api/v1/auth/register`
Register a new learner account.
- **Request Body**:
  ```json
  {
    "email": "learner@example.com",
    "password": "SecurePassword123!",
    "fullName": "Jane Doe"
  }
  ```
- **Response**: `201 Created` with user details (excluding password hash) and JWT token.

#### `POST /api/v1/auth/login`
Authenticate existing user.
- **Request Body**:
  ```json
  {
    "email": "learner@example.com",
    "password": "SecurePassword123!"
  }
  ```
- **Response**: `200 OK` with JWT token and user profile.

---

### 3.2 Problem Catalog (`/api/v1/problems`)

#### `GET /api/v1/problems`
List coding challenges with filtering and pagination.
- **Query Params**: `difficulty` (optional), `tag` (optional), `page` (default 0), `size` (default 20).
- **Response**: `200 OK` with paginated list of problem summary DTOs.

#### `GET /api/v1/problems/{slug}`
Retrieve problem details and starter template.
- **Response**: `200 OK`
- **Security Check**: Returns problem description, starter template, and ONLY visible sample test cases (`is_hidden = false`).

---

### 3.3 Submissions & Execution (`/api/v1/submissions`)

#### `POST /api/v1/submissions`
Submit code solution for execution.
- **Request Body**:
  ```json
  {
    "problemSlug": "two-sum",
    "language": "JAVA",
    "sourceCode": "public class Solution { public int[] twoSum(...) { ... } }"
  }
  ```
- **Response**: `202 Accepted`
  ```json
  {
    "success": true,
    "message": "Submission queued for execution",
    "data": {
      "submissionId": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
      "status": "PENDING"
    }
  }
  ```

#### `GET /api/v1/submissions/{id}`
Poll or retrieve submission result.
- **Response**: `200 OK`
  ```json
  {
    "success": true,
    "data": {
      "id": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
      "status": "WRONG_ANSWER",
      "testsPassed": 2,
      "totalTests": 5,
      "executionTimeMs": 140,
      "memoryUsedKb": 18400,
      "submittedAt": "2026-09-23T12:00:00Z"
    }
  }
  ```

---

### 3.4 AI Socratic Mentor (`/api/v1/mentor`)

#### `POST /api/v1/mentor/hints`
Request progressive Socratic hint for a submission.
- **Request Body**:
  ```json
  {
    "submissionId": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
    "preferredLevel": 1
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "success": true,
    "data": {
      "hintLevel": 1,
      "socraticStage": "NUDGE",
      "mentorMessage": "Notice how your loop boundary interacts with the last index. What happens when i reaches array.length?",
      "structuredFocus": {
        "concept": "Off-by-one boundary checking",
        "question": "Can an array of length N be indexed at position N?"
      }
    }
  }
  ```
- **Constraint**: Under no circumstances does the response contain the working code replacement.
