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
export OPENROUTER_MODEL="openai/gpt-5.2"
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
      "domain": "finance"
    }
  }'
```

Session APIs:

```bash
curl -X POST "http://localhost:8080/api/v1/sessions/start" \
  -H "Content-Type: application/json" \
  -d '{
    "businessRequest":"构建合同审批系统，支持多级审批和移动端",
    "domain":"finance"
  }'
```

## Public Deployment (GitHub + Render)

This repository is public and can be deployed directly from GitHub.

1. Open Render Dashboard and create a new Blueprint service.
2. Connect GitHub repo: `bryan4liwenqiang/paradigm-sys-1`.
3. Render will detect `render.yaml` and create the web service automatically.
4. In Render environment variables, set:
   - `OPENROUTER_API_KEY` (required)
5. Deploy and open the generated public URL.

Local Docker run (optional):

```bash
docker build -t paradigm-sys-1 .
docker run --rm -p 8080:8080 \
  -e OPENROUTER_API_KEY="your_key" \
  -e OPENROUTER_MODEL="openai/gpt-5.2" \
  paradigm-sys-1
```

## Spring Boot Mode

If your environment can access Maven repositories and has Docker/PostgreSQL:

```bash
docker compose up -d
./mvnw spring-boot:run
```
