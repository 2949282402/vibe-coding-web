# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

This repository is a Docker-first personal blog application with:
- a Vue 3 + Vite frontend (`frontend/`, `frontend/package.json`)
- a Spring Boot 3 + MyBatis + JWT backend (`backend/`, `backend/pom.xml`)
- MySQL, Redis, and Qdrant wired through Docker Compose (`docker-compose.yml`)
- an optional Python `llm-bridge` profile for local model experiments (`llm-bridge/`, `docker-compose.yml`)

## Common commands

### Full stack
- Start the full stack: `docker compose up -d --build`
- Stop the stack: `docker compose down`
- Start with the optional bridge profile: `docker compose --profile llm-bridge up -d --build`

### Frontend (`frontend/`)
- Install dependencies: `npm install`
- Start the Vite dev server: `npm run dev`
- Build for production: `npm run build`
- Preview the production build: `npm run preview`

### Backend (`backend/`)
- Start the Spring Boot app: `mvn spring-boot:run`
- Run all tests: `mvn test`
- Run a single test class: `mvn -Dtest=ClassNameTest test`
- Build without tests: `mvn -DskipTests package`

### Notable absences
- There is no frontend lint script in `frontend/package.json`.
- There is no frontend test script in `frontend/package.json`.

## Architecture overview

- `frontend/src/router/index.js` defines a public site under `MainLayout`, a dedicated `/knowledge` route for the RAG UI, and JWT-protected `/admin` routes under `AdminLayout`.
- The frontend uses relative `/api` requests, and local split development expects the backend on `:8080` with dev proxying from Vite (`README.md`).
- `backend/src/main/resources/application.yml` is the main backend configuration hub for datasource, Redis cache, JWT, and RAG feature toggles.
- The backend includes an embedded RAG subsystem under `backend/src/main/java/com/hejulian/blog/rag/` with separate application, domain, infrastructure, and config layers rather than a standalone service.
- `frontend/src/api/blog.js` implements streaming knowledge-chat requests with `fetch` and manual SSE parsing instead of `EventSource`.
- Persistence is split between static SQL and runtime initialization:
  - base blog schema lives in `sql/blog_mysql_init.sql`
  - RAG tables are created/evolved at startup by `backend/src/main/java/com/hejulian/blog/rag/infrastructure/persistence/RagSchemaInitializer.java`

## High-value paths

- `README.md`
- `docker-compose.yml`
- `frontend/src/router/index.js`
- `frontend/src/api/blog.js`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/hejulian/blog/rag/`
- `backend/src/main/java/com/hejulian/blog/rag/infrastructure/persistence/RagSchemaInitializer.java`
- `sql/blog_mysql_init.sql`

## Repo-specific cautions

- Many RAG capabilities are feature-flagged through environment variables in `docker-compose.yml` and `backend/src/main/resources/application.yml`; verify which toggles are enabled before changing behavior.
- If a persistence change touches RAG tables, check whether it belongs in `sql/blog_mysql_init.sql`, `RagSchemaInitializer.java`, or both.
- If you change streaming chat behavior, keep the backend SSE contract and the frontend parser in sync (`frontend/src/api/blog.js`).
