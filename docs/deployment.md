# Production Deployment Strategy

## 1. Deployment Topology

The target production infrastructure balances ease of maintenance with cost-effectiveness:

| Component | Target Hosting Platform | Deployment Strategy |
|---|---|---|
| **Frontend** | [Vercel](https://vercel.com) | Automated Git-triggered builds from `frontend/` directory. |
| **Backend** | Container-compatible platform (Render, Railway, Fly.io, AWS ECS) | Multi-stage Docker container deployed from `backend/Dockerfile`. |
| **Database** | Managed PostgreSQL (Supabase, Neon, AWS RDS) | Flyway automated migrations during backend boot. |
| **Redis** | Managed Redis (Upstash, Redis Cloud) | High-performance cache and distributed rate limiting. |
| **Code Executor** | Dedicated isolated host / worker node (Fly.io Machines, AWS Fargate) | Runs isolated Docker sandbox with zero network and strict cgroups. |
| **AI Engine** | Self-hosted Ollama / vLLM on GPU instance (RunPod, Lambda Labs, Hetzner GPU) | Private endpoint accessible by backend without vendor lock-in. |

---

## 2. Frontend Deployment on Vercel

The frontend is structured to be deployed independently on Vercel with zero backend coupling:
- **Root Directory**: `frontend`
- **Framework Preset**: `Vite`
- **Build Command**: `npm run build`
- **Output Directory**: `dist`
- **Environment Variables**:
  - `VITE_API_BASE_URL`: Public URL of the backend API (e.g. `https://api.aicodingmentor.com/api/v1`).

### Vercel Independence Guarantee
- No Vercel Edge Functions or Vercel-specific proprietary APIs are used.
- Standard client-side routing handled via React Router and Vite static asset compilation.

---

## 3. Backend Containerization

The Spring Boot backend is packaged as an optimized lightweight container image:
```dockerfile
# Multi-stage build
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
```

---

## 4. Environment Variables Checklist

Ensure the following variables are configured in the target production environment:
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
- `JWT_SECRET` (Cryptographically random 256-bit secret)
- `OLLAMA_BASE_URL` (Secure private endpoint for AI inference)
- `OLLAMA_MODEL` (e.g., `qwen2.5-coder`)
