# Deployment Reference

Use for Docker, nginx, runtime config, logs, uploads exposure, SSE buffering, and static delivery.

## Root deployment files

- Compose: `docker-compose.yml`
- Backend image: `backend/Dockerfile`, `backend/docker-entrypoint.sh`
- Frontend image: `frontend/Dockerfile`, `frontend/docker-entrypoint.sh`
- Frontend nginx template: `frontend/nginx/default.conf.template`
- Checked-in nginx config: `frontend/nginx/default.conf`
- Optional external nginx sample: `deploy/nginx/hejulian-blog.conf`
- Runtime config: `backend/src/main/resources/application.yml`
- SQL bootstrap: `sql/blog_mysql_init.sql`
- Logs: `logs/YYYY-MM-DD/`

## Docker services

Typical services:

- `mysql`
- `redis`
- `qdrant`
- `backend`
- `frontend`
- optional `llm-bridge`

## Frontend delivery

The frontend container:

- builds Vue app with `npm run build`.
- serves `dist/` with nginx.
- generates `/etc/nginx/conf.d/default.conf` from `frontend/nginx/default.conf.template` at container start.
- serves `/resume` static files via mounted `frontend/public/resume/`.
- serves public images via mounted `frontend/public/images/`.

Important: for Docker builds, edit `default.conf.template`; changing only `default.conf` does not affect the generated container config.

## Backend delivery

The backend container:

- builds with Maven inside Docker.
- exposes JSON APIs, RAG/Ask SSE, Agent SSE, uploads, and actuator health.
- writes dated logs through mounted `./logs`.

## SSE routes that must not buffer

- RAG/Ask stream: `/api/public/rag/ask/stream`
- Agent task stream: `/api/agent/tasks/{taskId}/stream`

Nginx needs:

- `proxy_buffering off;`
- `proxy_cache off;`
- long `proxy_read_timeout` and `proxy_send_timeout`
- `add_header X-Accel-Buffering no;`

## Runtime model config

- System/env defaults are in `docker-compose.yml` and `application.yml`.
- User Qwen API key/model/capability live in user settings through auth APIs.
- `DASHSCOPE_API_KEY` can provide system-level fallback, but user runtime is preferred for RAG/Ask/Agent chat.

## Logs

Use dated directories under `logs/YYYY-MM-DD/`.
Typical files include backend/frontend/mysql/redis/qdrant logs depending on running services.

## Validation commands

- Frontend only: `npm run build` in `frontend/`.
- Backend only: `docker compose build backend` from repo root.
- Full rebuild: `docker compose up -d --build`.
- Restart existing built services: `docker compose up -d backend frontend`.

## Common pitfalls

- Editing generated nginx config but not the template.
- Forgetting container recreation after nginx/template changes.
- Assuming `/resume` is a Vue route.
- Debugging SSE only in Java while nginx is buffering.
- Forgetting Docker build context: frontend template is copied into the image.