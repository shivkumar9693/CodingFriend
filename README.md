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

## 🌟 Key Features

### 🐞 1. Live Step Debugger & Visual Memory Canvas
- **Line-by-Line Code Stepping**: Step forward, step backward, or auto-play through Java execution synchronized with Monaco Editor highlights.
- **Dynamic Multi-Structure Visualizers**:
  - **1D / 2D Arrays & Pointers**: Visual array representation with animated pointers (`left`, `right`, `slow`, `fast`, `i`, `j`, `mid`).
  - **Hash Maps & Hash Sets**: Interactive key-value buckets and collision tracking.
  - **Dynamic Programming (DP) Table**: Grid-based memoization matrix showing cell updates and recurrence values.
  - **Binary Trees (SVG Canvas)**: Hierarchical node rendering with left/right branch links, subtree highlights, and visited node indicators.
  - **Graphs (SVG Canvas)**: Node-edge topology graph visualizing BFS/DFS traversals and adjacency lists.
- **Call Stack & Variable Watch**: Real-time stack frames and primitive/object state inspector.
- **Socratic Checkpoints**: Pauses at crucial algorithmic decision branches to present pedagogical multiple-choice checks with instant feedback.
- **Side-by-Side UI**: Seamless left-panel visualizer docked next to the code editor with auto-scrolling execution logs.

### 🤖 2. Progressive Socratic AI Mentorship & SocraBuddy
- **Multi-Tiered Progressive Hints**: 
  - **Level 1 (Gentle Nudge)**: Asks an observational question to direct focus.
  - **Level 2 (Conceptual Insight)**: Explains the underlying computer science pattern or edge case.
  - **Level 3 (Algorithmic Blueprint)**: Outlines the high-level strategy without revealing code.
  - **Level 4 (Targeted Pointer)**: Pins down the exact logic or boundary condition flaw.
- **Mistake Engine & Error Post-Mortems**: Automatically classifies and explains Syntax, Logical, Runtime (NPE, OOB), and Time Limit Exceeded (TLE) errors.
- **SocraBuddy Live Companion**: Real-time inline code assistance and pedagogical feedback as learners write Java solutions.
- **100% Free & Self-Hostable**: Powered locally by open-weight models (`qwen2.5-coder`, `deepseek-coder`, `llama3`) via Ollama with zero paid API keys or external lock-in.

### ⚔️ 3. Real-Time 1v1 Coding Duels
- **Live Matchmaking**: Instant matchmaking queue for 1v1 live algorithm showdowns.
- **Interactive Duel Arena**: Real-time duel state machine tracking opponent progress (test cases passed, submission state) with live timer.
- **Competitive Rating System**: Dynamic Elo-style rating adjustments and winner/loser post-duel breakdown.

### 🏆 4. Contests & Competitive Arena
- **Virtual & Live Contests**: Timed multi-problem coding competitions with synchronized countdowns.
- **ICPC-Style Leaderboard**: Real-time ranking calculated using solved problem count and penalty minute scoring for failed attempts.
- **Editorial & Contest History**: Post-contest solution walkthroughs, problem review, and score archiving.

### 🧠 5. Spaced Repetition & AI Post-Mortem Revision Notes
- **Automated AI Post-Mortem Generation**: Deep failure analysis on unsuccessful submissions with counter-example diagnosis (Failed Input, Expected vs Actual Output).
- **Execution Flow Contrast**: Visual breakdown comparing the learner's actual code trace against the intended invariant trace to pinpoint algorithmic flaws.
- **Personalized Revision Notebook**: 1-click saving of post-mortems to a dedicated personal notebook with custom learner reflections, takeaways, and bookmarks.
- **SM-2 Spaced Repetition Engine**: Scientifically schedules problem revision intervals (`Due for Review`, `To Revise`, `Mastered`) based on learner performance and recall difficulty.
- **Mistake Taxonomy & Multi-Filter Search**: Categorizes errors into standardized buckets (Logic Error, Boundary Condition, Off-by-One, Runtime Exception, Complexity Detour) with instant full-text and tag search.
- **Concept Mastery Radar**: Visual metrics tracking learner proficiency and retention across core algorithmic patterns (Two Pointers, Sliding Window, DP, Trees, Graphs, Sorting).

### 🛡️ 6. Sandboxed Code Execution & Async Pipeline
- **Zero Host Execution**: Untrusted learner code is strictly isolated from the main Spring Boot JVM.
- **Dockerized Execution Sandbox**: Ephemeral runner with strict cgroup limits (256MB RAM cap, 1 CPU core quota, 5-second timeout, `--network none`, non-root user).
- **Kafka Async Queue**: Asynchronous submission pipeline decoupling client requests from execution workers for high-throughput scaling.
- **Hidden Test Privacy**: Public sample test cases are validated on client requests while strict grading suites remain hidden on the server.

### 📊 7. Big-O Complexity Radar & Detour Detection
- **Algorithmic Complexity Estimation**: Evaluates Time ($O(N)$, $O(N \log N)$, $O(N^2)$) and Space ($O(1)$, $O(N)$) asymptotic complexity.
- **Detour Warning System**: Detects when a learner's solution takes an overly complex algorithmic path compared to optimal problem constraints.

### 🔐 8. Enterprise Security & Admin Suite
- **Stateless JWT Security**: BCrypt password hashing, database-backed refresh token rotation/revocation, and role-based access control (`ROLE_USER`, `ROLE_ADMIN`).
- **Comprehensive Admin Suite**: Full problem CRUD, test case authoring, support ticket management, and learner moderation.
- **Spring Boot 3 Modular Monolith**: Clean module boundaries (`auth`, `problem`, `submission`, `execution`, `mentor`, `mistake`, `learning`), PostgreSQL 16+ with Flyway migrations, and Redis caching.

---

## Tech Stack

### Backend
- **Framework**: Spring Boot 3.3.x (Java 17)
- **Database**: PostgreSQL 16+ with Flyway migrations
- **Cache & Rate Limiting**: Redis
- **Message Broker**: Apache Kafka (asynchronous submission execution queue)
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

### 2. Start Complete Stack via Docker Compose
```bash
# Start all 7 services (Postgres, Redis, Kafka, Ollama, Code-Executor, Backend, Frontend)
docker compose up -d

# Or start backing infrastructure only for hybrid local development:
docker compose up -d postgres redis kafka ollama
```

### 3. Run Backend (If Running on Host)
```bash
cd backend
./mvnw spring-boot:run
```
The backend API is accessible at: `http://localhost:8080`  
Swagger UI documentation: `http://localhost:8080/swagger-ui.html`
Actuator Health endpoint: `http://localhost:8080/actuator/health`

### 4. Run Frontend (If Running on Host)
```bash
cd frontend
npm install
npm run dev
```
The web app is accessible at: `http://localhost:5173`

---

## 🧪 Automated Testing

### Backend Test Execution
```bash
# Run all backend unit, integration, and security tests (244+ tests)
cd backend
./mvnw test

# Run Security & Authentication test suites
./mvnw test -Dtest=SecurityIntegrationTest,AuthControllerTest,JwtTokenProviderTest

# Run Kafka Event & Message Pipeline tests
./mvnw test -Dtest="*Kafka*,*Consumer*,*Producer*"

# Run Code Execution & Sandbox isolation tests
./mvnw test -Dtest="*Execution*,*Evaluator*,*Executor*"

# Run Socratic AI & Mistake Engine tests
./mvnw test -Dtest="*Mentor*,*Mistake*,*Prompt*"
```

### Frontend Test Execution
```bash
# Run Vitest unit & component test suite
cd frontend
npm run test

# Run frontend build verification
npm run build
```

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

- [Future Product Vision & Strategic Technical Roadmap](docs/future-roadmap.md)
- [Architecture Overview](docs/architecture.md)
- [Requirements & Specifications](docs/requirements.md)
- [Database Schema & Migrations](docs/database-design.md)
- [REST API Specifications](docs/api-documentation.md)
- [AI Mentor & Socratic System](docs/ai-mentor.md)
- [Code Execution Sandbox](docs/code-execution.md)
- [Security Guidelines & Threat Audit](docs/security-audit.md)
- [Testing Strategy & Test Suite Guide](docs/testing.md)
- [Production Observability & Telemetry](docs/observability.md)
- [Development Setup Guide](docs/development-setup.md)
- [Deployment Strategy](docs/deployment.md)
- [Architecture Decision Records (ADRs)](docs/decisions/)

---

## ⚠️ Legal Notice

**This is a private project.** All source code, documentation, and materials are the exclusive property of Shivkumar.

For full licensing and copyright information, please refer to:
- [LICENSE](LICENSE) - All Rights Reserved License
- [COPYRIGHT.md](COPYRIGHT.md) - Detailed Copyright Notice

---
