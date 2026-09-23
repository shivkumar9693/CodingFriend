# 1. Modular Monolith Architecture Selection

Date: 2026-09-23

## Status
Accepted

## Context
We are designing **AI Coding Mentor**, an online platform where learners write Java solutions, receive automated evaluation, and consult an AI mentor for progressive Socratic hints.

Key operational considerations:
- Small engineering team building a production-style resume project.
- High domain cohesion between problem management, submissions, mistake evaluation, and AI hint generation.
- The platform requires isolated code execution and local LLM inference.
- Microservices introduce significant operational complexity (distributed transactions, inter-service networking, observability sprawl, multiple CI/CD pipelines, latency overhead).

## Decision
We decided to architect the backend application as a **Modular Monolith** using Java 17 and Spring Boot 3.x, while isolating:
1. **Untrusted Code Execution**: Hosted in dedicated, isolated Docker sandbox worker processes.
2. **AI Inference**: Delegated to a self-contained local Ollama instance over standard REST protocols.

Domain modules are strictly partitioned within packages under `com.aicodingmentor`:
- `auth`
- `user`
- `problem`
- `submission`
- `execution`
- `mistake`
- `mentor`
- `learning`
- `common`

Cross-module coupling is restricted to explicit public service interfaces and asynchronous event publishers.

## Consequences

### Positive
- **Simplicity**: Single codebase, single build, single database schema with atomic transactions.
- **Fast Iteration**: Refactoring across domain boundaries is safe and straightforward with IDE static analysis.
- **Cost Effective**: Can be deployed on a single modest server or container without multi-cluster overhead.
- **Future-Proof**: Clean module boundaries and asynchronous events (planned Kafka integration) allow extracting high-load modules (such as the execution worker) into independent microservices when scale demands it.

### Negative / Trade-offs
- Requires discipline to prevent developers from violating package boundaries or directly joining JPA tables across modules.
- The monolithic JVM must be restarted to deploy updates to any individual domain module.
