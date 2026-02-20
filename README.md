# Paradigm Sys

This repository contains two runtime modes:

- Spring Boot mode (full architecture, needs Maven dependencies)
- Standalone mode (zero dependency, JDK only, runs in restricted environments)

## Quick Start (Standalone)

Requirements:

- JDK 17+

Run:

```bash
./standalone/run.sh
```

Open UI:

```text
http://localhost:8080/
```

Optional OpenRouter integration (for requirement clarification chat):

```bash
export OPENROUTER_API_KEY="your_key"
export OPENROUTER_MODEL="openai/gpt-4o-mini"
./standalone/run.sh
```

Health check:

```bash
curl "http://localhost:8080/health"
```

Recommend API:

```bash
curl -X POST "http://localhost:8080/api/v1/mapping/recommend" \
  -H "Content-Type: application/json" \
  -d '{
    "projectId": 1001,
    "stageCode": "requirements",
    "context": {
      "domain": "finance",
      "complianceLevel": "L3",
      "timelineLevel": "tight"
    }
  }'
```

Session APIs:

```bash
curl -X POST "http://localhost:8080/api/v1/sessions/start" \
  -H "Content-Type: application/json" \
  -d '{
    "businessRequest":"构建合同审批系统，支持多级审批和移动端",
    "domain":"finance",
    "complianceLevel":"L3",
    "timelineLevel":"tight"
  }'
```

## Spring Boot Mode

If your environment can access Maven repositories and has Docker/PostgreSQL:

```bash
docker compose up -d
./mvnw spring-boot:run
```
