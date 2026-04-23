---
name: hejulian-web-editing
description: Use when modifying, debugging, reviewing, or extending the hejulian-web repository at E:\coding\hejulian-web, including its Vue frontend, Spring Boot backend, RAG/Ask/Agent knowledge page, Agent orchestration, admin console, MyBatis schema, Docker/nginx deployment, logs, and static resume delivery.
---

# Hejulian Web Editing

Use this skill only for `E:\coding\hejulian-web`.
Treat it as a project map and routing guide, not a full code dump.

## Core workflow

1. Classify the task by area: frontend, backend, RAG/Ask, Agent, deployment, resume, or review.
2. Read only the matching reference file under `references/`.
3. For cross-cutting work, load the second reference explicitly named below.
4. Before editing, trace the smallest complete chain from UI/API to service, persistence, cache, and deployment when applicable.
5. Keep changes targeted and preserve unrelated user edits.

## Project snapshot

- Root: `frontend/`, `backend/`, `llm-bridge/`, `sql/`, `deploy/`, `logs/`, `docs/`, `.agents/`, `docker-compose.yml`.
- Frontend: Vue 3 + Vite + Vue Router + Pinia + Element Plus.
- Backend: Spring Boot 3 + MyBatis + Spring Security JWT + Redis cache + MySQL.
- RAG/Ask/Agent live in the same app, not a separate service.
- `/knowledge` supports three response modes: `RAG`, `Ask`, and admin-only `Agent`.
- Qwen API key, selected model, and web-search capability are stored per user.
- Web search uses Qwen compatible chat completions with `enable_search=true`.
- Agent orchestration lives under `backend/src/main/java/com/hejulian/blog/agent` and is exposed through `/api/agent/**`.
- Static `/resume` is served by nginx from `frontend/public/resume/index.html`, not by Vue Router.

## Reference routing

### Frontend UI, routes, stores, API client

Read `references/frontend.md`.
Also read `references/rag.md` for `/knowledge`, chat modes, sources, or SSE.

### Backend APIs, services, auth, persistence, schema

Read `references/backend.md`.
Also read `references/rag.md` for chat/retrieval/model behavior or `references/deployment.md` for runtime wiring.

### `/knowledge`, RAG, Ask, history, sources, feedback, SSE

Read `references/rag.md` first.
Also read `references/frontend.md` if changing UI and `references/backend.md` if changing API/service/persistence contracts.

### Agent writing/publishing workflow, tools, trace, ops

Read `references/agent.md` first.
Also read `references/rag.md` for shared Qwen runtime/SSE behavior and `references/backend.md` for persistence/security.

### Docker, nginx, runtime config, logs, static serving

Read `references/deployment.md`.
Also read `references/resume.md` for `/resume` or `references/rag.md` for SSE buffering.

### Static resume

Read `references/resume.md`.
Do not assume `/resume` is a Vue route.

### Code review

Read `references/review.md`, then the affected-area reference.

## Cross-cutting guardrails

- Prefer Chinese for user-facing explanations in this repository.
- When visible text changes, update Chinese and English branches together when both exist.
- Encoding is mandatory: all project files and skill files must be treated as UTF-8.
- When reading or writing files from PowerShell, specify UTF-8 explicitly. Avoid default ANSI/GBK or locale-dependent encodings.
- For scripted writes, prefer UTF-8 without BOM when safe, for example `.NET UTF8Encoding($false)`. Do not rewrite existing UTF-8 files with a non-UTF-8 encoding.
- Do not bulk-read the repo. Use targeted files from the reference maps.
- Do not revert unrelated work; assume the worktree may already be dirty.
- For schema-impacting changes, check entity, mapper interface, mapper XML, service caller, SQL initializer, and schema compatibility initializer.
- For writes affecting public content, RAG sessions, history, Qwen config, or Agent tasks, check Redis cache invalidation.
- For SSE issues, check backend emitter, frontend `fetch` stream parser, and nginx `proxy_buffering off`.
- For Docker frontend nginx changes, update `frontend/nginx/default.conf.template`; `default.conf` alone is not enough for container builds.

## Preferred validation

- Frontend: `npm run build` in `frontend/`.
- Backend: local Maven if available, otherwise `docker compose build backend` from repo root.
- Full container refresh when needed: `docker compose up -d --build`.
- Do not run expensive validation unless the user asks or the change is compile-sensitive.
