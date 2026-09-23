# Local Development Setup Guide

This guide walks you through setting up a complete development environment for **AI Coding Mentor** on Windows, macOS, or Linux.

---

## 1. Prerequisites

Ensure you have installed:
- **Java 17+ JDK** (Eclipse Temurin, OpenJDK, or Oracle JDK)
- **Node.js 18+** & **npm 9+**
- **Docker** & **Docker Compose**
- **Git**
- **Ollama** (optional, recommended for testing AI hints locally: [ollama.com](https://ollama.com))

---

## 2. Step-by-Step Setup

### Step 1: Clone Repository & Create Environment File
```bash
git clone https://github.com/ai-coding-mentor/ai-coding-mentor.git
cd ai-coding-mentor
cp .env.example .env
```

### Step 2: Start Infrastructure Services
Start PostgreSQL and Redis in the background:
```bash
docker-compose up -d postgres redis
```
Verify containers are healthy:
```bash
docker-compose ps
```

### Step 3: Setup Local AI (Ollama)
If using a local Ollama instance:
1. Start Ollama:
   ```bash
   ollama serve
   ```
2. Pull the recommended open coding model:
   ```bash
   ollama pull qwen2.5-coder:latest
   ```

*(Alternatively, you can run Ollama via docker-compose: `docker-compose up -d ollama`)*.

### Step 4: Run Backend (Spring Boot)
Open a terminal in `backend/`:
```bash
cd backend
# Windows:
mvnw.cmd spring-boot:run
# macOS/Linux:
./mvnw spring-boot:run
```
- API Base: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Actuator Health: `http://localhost:8080/actuator/health`

### Step 5: Run Frontend (React + Vite)
Open a new terminal in `frontend/`:
```bash
cd frontend
npm install
npm run dev
```
- Web Application: `http://localhost:5173`

---

## 3. Running Automated Tests

### Run Backend Tests
```bash
cd backend
# Windows:
mvnw.cmd test
# macOS/Linux:
./mvnw test
```

### Run Frontend Tests and Build
```bash
cd frontend
npm run build
```

---

## 4. Troubleshooting Common Issues

- **Database Connection Error**: Ensure PostgreSQL is running (`docker-compose ps`) and credentials in `.env` match `docker-compose.yml`.
- **Port 5432 or 6379 Already in Use**: If you have existing local instances of PostgreSQL or Redis, adjust the host port mapping in `.env` (e.g. `DB_PORT=5433`).
- **Ollama Not Responding**: Test if Ollama is reachable:
  ```bash
  curl http://localhost:11434/api/tags
  ```
