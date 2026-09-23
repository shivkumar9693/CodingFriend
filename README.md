# AI Coding Mentor

[![CI Build](https://github.com/ai-coding-mentor/ai-coding-mentor/actions/workflows/ci.yml/badge.svg)](https://github.com/ai-coding-mentor/ai-coding-mentor/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-blue.svg)](https://www.typescriptlang.org/)

> An online coding platform where learners solve Java coding problems and an AI mentor analyzes their mistakes to provide progressive Socratic hints instead of immediately revealing the solution.

---

## Key Features

- **Progressive Socratic Mentorship**: Multi-tier hints (Nudge $\to$ Conceptual $\to$ Algorithmic $\to$ Targeted) powered by local LLMs via Ollama.
- **Isolated Code Execution**: Secure, sandboxed code runner isolating untrusted learner code from the core application JVM.
- **Production-Ready Architecture**: Modular monolith backend built with Spring Boot 3, PostgreSQL, Redis, and Flyway.
- **Modern Interactive Frontend**: Fast, responsive React + TypeScript workspace featuring Monaco Editor.
- **Zero Paid AI Dependency**: 100% self-hostable with open-weight models (`qwen2.5-coder`, `deepseek-coder`, etc.) running on Ollama.
- **Strict Privacy & Security**: Hidden test cases never leak to the client; user code never runs inside the main application runtime.

---

## Tech Stack

### Backend
- **Framework**: Spring Boot 3.3.x (Java 17)
- **Database**: PostgreSQL 16+ with Flyway migrations
- **Cache & Rate Limiting**: Redis
- **Message Broker (Planned)**: Apache Kafka
- **Security**: Spring Security + JWT
- **API Spec**: Springdoc OpenAPI / Swagger UI
- **Testing**: JUnit 5, Mockito, Testcontainers

### Frontend
- **Framework**: React 18 / 19 + TypeScript + Vite
- **Code Editor**: Monaco Editor (`@monaco-editor/react`)
- **Routing**: React Router
- **Deployment**: Vercel-ready static build

### AI & Sandbox
- **LLM Runtime**: [Ollama](https://ollama.ai) (local inference)
- **Execution Sandbox**: Dockerized isolated Java runner with restricted cgroups, timeout, and network isolation

---

## Repository Structure

```
ai-coding-mentor/
├── backend/            # Spring Boot 3 modular monolith
│   ├── src/main/java/  # Application domain modules (auth, user, problem, submission, etc.)
│   └── src/test/java/  # Unit, integration, and Testcontainers suites
├── frontend/           # React + Vite + TypeScript web application
│   ├── src/            # Components, pages, Monaco editor, API client
│   └── index.html      # Single page app root
├── code-executor/      # Sandboxed runner Dockerfile and worker specs
├── docs/               # Comprehensive architecture, API, and setup documentation
├── .github/workflows/  # CI pipelines for backend and frontend
├── docker-compose.yml  # Local multi-container development environment
├── AGENTS.md           # Engineering principles and rules for AI contributors
├── .env.example        # Environment variable definitions
├── CONTRIBUTING.md     # Contribution guidelines and coding conventions
└── LICENSE             # MIT License
```

---

## Quick Start (Local Development)

### Prerequisites
- **Java 17+** (JDK)
- **Node.js 18+** & **npm**
- **Docker** & **Docker Compose**
- **Ollama** (optional for local AI features: `ollama run qwen2.5-coder`)

### 1. Clone & Configure Environment
```bash
cp .env.example .env
```

### 2. Start Supporting Infrastructure via Docker Compose
```bash
docker-compose up -d postgres redis ollama
```

### 3. Run Backend
```bash
cd backend
./mvnw spring-boot:run
```
The backend API is accessible at: `http://localhost:8080`  
Swagger UI documentation: `http://localhost:8080/swagger-ui.html`

### 4. Run Frontend
```bash
cd frontend
npm install
npm run dev
```
The web app is accessible at: `http://localhost:5173`

---

## Documentation

Comprehensive project documentation is available in the [`docs/`](file:///c:/Users/hp/OneDrive/Desktop/CodingFriend/docs/) directory:

- [Architecture Overview](file:///c:/Users/hp/OneDrive/Desktop/CodingFriend/docs/architecture.md)
- [Requirements & Specifications](file:///c:/Users/hp/OneDrive/Desktop/CodingFriend/docs/requirements.md)
- [Database Schema & Migrations](file:///c:/Users/hp/OneDrive/Desktop/CodingFriend/docs/database-design.md)
- [REST API Specifications](file:///c:/Users/hp/OneDrive/Desktop/CodingFriend/docs/api-documentation.md)
- [AI Mentor & Socratic System](file:///c:/Users/hp/OneDrive/Desktop/CodingFriend/docs/ai-mentor.md)
- [Code Execution Sandbox](file:///c:/Users/hp/OneDrive/Desktop/CodingFriend/docs/code-execution.md)
- [Security Guidelines](file:///c:/Users/hp/OneDrive/Desktop/CodingFriend/docs/security.md)
- [Development Setup Guide](file:///c:/Users/hp/OneDrive/Desktop/CodingFriend/docs/development-setup.md)
- [Deployment Strategy](file:///c:/Users/hp/OneDrive/Desktop/CodingFriend/docs/deployment.md)
- [Testing Strategy](file:///c:/Users/hp/OneDrive/Desktop/CodingFriend/docs/testing.md)
- [Architecture Decision Records (ADRs)](file:///c:/Users/hp/OneDrive/Desktop/CodingFriend/docs/decisions/)

---

## License

This project is licensed under the [MIT License](LICENSE).
