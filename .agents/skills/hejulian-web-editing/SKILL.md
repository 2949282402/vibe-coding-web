---
name: hejulian-web-editing
description: Use this skill only when modifying, debugging, reviewing, or extending the hejulian-web repository at E:\coding\hejulian-web. It routes tasks to the right repo-specific references for the Vue frontend, Spring Boot backend, RAG chat, Docker/nginx deployment, and the static online resume.
---

# Hejulian Web Editing

Use this skill only for the `hejulian-web` repository at `E:\coding\hejulian-web`.
If the task is for another repo, do not use this skill.

## Purpose

This skill is a repo router, not a full dump of the whole codebase.

Use it to:

- identify which part of this repo a task belongs to
- load only the relevant repo context for that task
- preserve known product assumptions around `/knowledge`, auth, caching, deployment, and `/resume`

## How to use this skill

Follow this order:

1. Confirm the task category.
2. Read `Project snapshot`.
3. Read only the matching file in `references/`.
4. Read extra reference files only if the task crosses boundaries.

Do not load every reference by default.

## When this skill should trigger

Use it for requests such as:

- modify the public blog frontend or backend in this repo
- adjust the `/knowledge` RAG page or its backend contract
- add or fix admin, article, taxonomy, comment, upload, auth, or feedback features
- change RAG history, sources, search mode, session lifecycle, or retrieval behavior
- edit Docker, nginx, static asset delivery, or `/resume`
- review code changes in this repo with repo-specific context

## Project snapshot

- Monorepo-style app with `frontend/`, `backend/`, optional `llm-bridge/`, and root `docker-compose.yml`
- Frontend: Vue 3 + Vite + Vue Router + Pinia + Element Plus
- Backend: Spring Boot 3 + MyBatis + Spring Security + JWT + Redis cache
- Main product areas: public blog, admin console, login/profile, uploads, authenticated RAG chat, and static online resume
- RAG lives inside the backend, not in a separate service
- Redis is used for Spring Cache, including public content caches, RAG history or session caches, and cached Qwen capability results
- User login is required for comments and RAG chat; public browsing remains open
- Qwen API key, selected model, and related capability state are stored per user
- Current web search path uses Qwen compatible chat completions with `enable_search=true`
- Static online resume is served from `frontend/public/resume/index.html` through nginx and Docker bind mounts, not Vue Router

## Task routing

Read only the subsection that matches the task.

### Frontend task

Use for Vue pages, layouts, routes, UI, theme, copy, or client-side interaction.

Read:

- `references/frontend.md`

Also read if needed:

- `references/rag.md` for `/knowledge`
- `references/resume.md` for `/resume`

### Backend task

Use for controllers, services, auth, uploads, public APIs, admin APIs, persistence, or schema work.

Read:

- `references/backend.md`

Also read if needed:

- `references/rag.md` for chat or retrieval behavior
- `references/deployment.md` for runtime wiring or static serving

### RAG or `/knowledge` task

Use for chat, retrieval, search mode, history, sources, replay, feedback, session lifecycle, or SSE behavior.

Read:

- `references/rag.md`

Also read if needed:

- `references/frontend.md`
- `references/backend.md`

### Deployment or static delivery task

Use for Docker, nginx, uploads exposure, bind mounts, runtime config, or static asset delivery.

Read:

- `references/deployment.md`

Also read if needed:

- `references/resume.md` for `/resume`
- `references/backend.md` for uploads or runtime config

### Resume task

Use only when the task is about the online resume at `/resume`.

Read:

- `references/resume.md`

Also read if needed:

- `references/deployment.md`

### Review task

Use when reviewing code changes in this repo.

Read:

- `references/review.md`

Then load only the additional references needed by the affected files.

## Cross-cutting guardrails

These apply regardless of task type.

### Working style

Before changing code:

1. Give a short numbered task list first.
2. Follow that list strictly.
3. Do not bundle unrelated feature changes.

While working:

- Prefer Chinese in user-facing communication.
- Keep edits small and targeted.
- Preserve existing behavior unless the user asked to change it.
- Assume the git worktree may already be dirty and never revert unrelated changes.

### Locale

If visible text changes:

- update both Chinese and English when both exist
- Chinese mode should read naturally
- do not add visible English-only text to Chinese UI by accident

### Persistence

If schema or stored data changes, do not assume one file is enough.
Check whether the change belongs in:

- `sql/blog_mysql_init.sql`
- mapper XML
- repository or service code
- `UserSchemaInitializer.java`
- `RagSchemaInitializer.java`

### Caching and refresh behavior

Be careful with refresh cost and cache invalidation.

Current expectations:

- `GET /api/auth/qwen-config` should behave like a read-mostly endpoint
- page refresh should not re-probe all remote model capabilities
- `POST /api/auth/qwen-config` is the right place to refresh capability detection
- RAG session and history writes should evict related caches
