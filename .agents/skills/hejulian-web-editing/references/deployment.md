# Deployment Reference

Read this file only for Docker, nginx, uploads exposure, bind mounts, or runtime delivery tasks.

## Deployment structure

Main files:

- root orchestration: `docker-compose.yml`
- frontend image: `frontend/Dockerfile`
- frontend nginx template: `frontend/nginx/default.conf.template`
- checked-in nginx config: `frontend/nginx/default.conf`
- frontend entrypoint: `frontend/docker-entrypoint.sh`
- backend uploads exposure: `backend/src/main/java/com/hejulian/blog/config/WebConfig.java`
- backend runtime config: `backend/src/main/resources/application.yml`

Optional local model bridge:

- `llm-bridge/`

## Docker compose structure

The project typically runs with these services:

- `mysql`
- `redis`
- `qdrant`
- `backend`
- `frontend`
- optional `llm-bridge`

When editing `docker-compose.yml`, check:

- ports
- environment variables
- bind mounts
- volume mounts
- health checks
- service dependencies

## Frontend delivery

The frontend container serves:

- built Vue app from nginx
- `/resume` static page via mounted files under `frontend/public/resume/`
- resume avatar or related assets via mounted files under `frontend/public/images/`

Important assumptions:

- `/resume` is served by nginx, not Vue Router
- mounted resume content should refresh without rebuilding the frontend image
- nginx config changes usually require recreating the frontend container once

## Backend delivery

The backend serves:

- JSON APIs
- RAG APIs
- SSE endpoint for streaming chat
- uploaded files exposed through `WebConfig.java`

If the task changes file exposure, check both backend resource mapping and frontend or nginx path assumptions.

## Nginx structure

Start with:

- `frontend/nginx/default.conf.template`
- `frontend/nginx/default.conf`

Typical responsibilities:

- serve the SPA
- proxy `/api/**`
- proxy uploads or health endpoints if configured
- special-case static resume route

If the task changes route behavior, inspect both the template and generated config in the repo.

## Runtime config

Read `backend/src/main/resources/application.yml` when changing:

- service ports
- datasource defaults
- Redis config
- Qdrant config
- RAG model or timeout settings
- upload directory defaults

## Upload and static file delivery

For upload-related delivery, inspect:

- `backend/src/main/java/com/hejulian/blog/controller/admin/AdminUploadController.java`
- `backend/src/main/java/com/hejulian/blog/service/UploadStorageService.java`
- `backend/src/main/java/com/hejulian/blog/config/WebConfig.java`

For resume-related delivery, inspect:

- `frontend/public/resume/index.html`
- `frontend/public/images/`
- `docker-compose.yml`
- nginx config

## Validation

For deployment or delivery changes, verify what applies:

- path in repo matches path mounted into container
- nginx route matches expected URL
- container recreation is done if config or mounts changed
- frontend app routes and static routes are not fighting each other

If the task is specifically about `/resume`, also read `references/resume.md`.
