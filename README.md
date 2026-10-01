# AI Coding Mentor

> 🔒 **⚠️ PRIVATE PROJECT - All Rights Reserved**
>
> This repository is the exclusive property of **Shivkumar** and is intended for personal use only.
> This project is **NOT open source** and may **NOT** be copied, modified, distributed, forked, or used without explicit written permission.
> For full licensing details, see [LICENSE](LICENSE) and [COPYRIGHT.md](COPYRIGHT.md).

---

[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-blue.svg)](https://www.typescriptlang.org/)

> An online coding platform where learners solve Java coding problems and an AI mentor analyzes their mistakes to provide progressive Socratic hints instead of immediately revealing the solution.

---

## 📋 Repository Status

- **Owner:** Shivkumar ([@shivkumar9693](https://github.com/shivkumar9693))
- **License:** All Rights Reserved
- **Status:** Private Project
- **Intended Use:** Personal / Private Use Only
- **Public Contribution:** NOT Accepted
- **Forks:** NOT Permitted

---

## Key Features

- **Authentication & Authorization**: Stateless JWT security with BCrypt password hashing, database-backed refresh token rotation/revocation, and role-based access control (`USER` and `ADMIN`).
- **Coding Problem Management**: Full problem catalog with pagination, difficulty & tag filtering, Java starter templates, and admin problem curation with secure hidden test suite isolation.
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
CodingFriend/
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
├── LICENSE             # All Rights Reserved License
├── COPYRIGHT.md        # Copyright and ownership notice
└── .github/CODEOWNERS  # Repository code ownership
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

## 🚀 Frontend Vercel Deployment

The frontend is ready for static deployment on [Vercel](https://vercel.com/) with zero backend coupling:

### 1. Import Project to Vercel
1. Push your repository to GitHub / GitLab.
2. In the Vercel Dashboard, select **"New Project"** and import your repository.
3. Set the **Root Directory** to `frontend`.
4. Leave Framework Preset as **Vite**.

### 2. Configure Environment Variables in Vercel
In the Vercel project settings, set:
- `VITE_API_BASE_URL`: The public HTTPS URL of your deployed Spring Boot backend (e.g., `https://api.yourdomain.com`).

### 3. SPA Routing & Deployment
- The SPA rewrite rule is pre-configured in `frontend/vercel.json` (`rewrites: [{"source": "/(.*)", "destination": "/index.html"}]`) to support client-side routing on page refresh.
- Click **Deploy**. Vercel will run `npm run build` and publish the `dist/` directory.

For full frontend documentation, see [frontend/README.md](frontend/README.md).

---

## Documentation

Comprehensive project documentation is available in the [`docs/`](docs/) directory:

- [Architecture Overview](docs/architecture.md)
- [Requirements & Specifications](docs/requirements.md)
- [Database Schema & Migrations](docs/database-design.md)
- [REST API Specifications](docs/api-documentation.md)
- [AI Mentor & Socratic System](docs/ai-mentor.md)
- [Code Execution Sandbox](docs/code-execution.md)
- [Security Guidelines](docs/security.md)
- [Development Setup Guide](docs/development-setup.md)
- [Deployment Strategy](docs/deployment.md)
- [Testing Strategy](docs/testing.md)
- [Architecture Decision Records (ADRs)](docs/decisions/)

---

## ⚠️ Legal Notice

**This is a private project.** All source code, documentation, and materials are the exclusive property of Shivkumar.

For full licensing and copyright information, please refer to:
- [LICENSE](LICENSE) - All Rights Reserved License
- [COPYRIGHT.md](COPYRIGHT.md) - Detailed Copyright Notice

---
