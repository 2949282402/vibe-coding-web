# Backend Reference

Use for controllers, services, auth, uploads, public/admin APIs, MyBatis persistence, schema, cache, and runtime config.

## Stack

- Spring Boot 3
- Java 21 container build
- MyBatis XML mappers
- Spring Security + JWT
- Redis-backed Spring Cache
- MySQL

## Package map

Base package: `backend/src/main/java/com/hejulian/blog/`

- `common/`: API response, page response, cache names.
- `config/`: Spring Security, CORS, Redis, web/static mappings.
- `controller/`: public/auth/RAG controllers.
- `controller/admin/`: admin dashboard, posts, taxonomy, comments, feedback, uploads.
- `dto/`: REST DTOs such as `AuthDtos`, `RagDtos`, `AdminDtos`.
- `entity/`: persistence entities.
- `mapper/`: MyBatis mapper interfaces.
- `service/`: blog, auth, uploads, schema/data initialization, optional Python bridge.
- `rag/`: RAG/Ask application, domain, model gateway, vector persistence.
- `agent/`: Agent tasks, orchestration, tools, trace, memory, ops.

## Important controllers

- `PublicBlogController.java`: homepage, post list/detail, comments, public RAG search.
- `AuthController.java`: login/register/profile/Qwen config.
- `RagController.java`: `/api/public/rag/**`, Ask/RAG normal and SSE chat.
- `AgentTaskController.java`: `/api/agent/tasks/**`, admin-only task and SSE endpoint.
- Agent/admin controllers under `backend/src/main/java/com/hejulian/blog/agent/controller/`.

## Service responsibilities

- `AuthService.java`: account/profile/Qwen runtime options.
- `AdminBlogService.java`: post CRUD and publishing path used by Agent publisher.
- `PublicBlogService.java`: public site aggregation/detail/search.
- `RagApplicationService.java`: RAG, Ask, SSE, history, replay, feedback.
- `AgentOrchestratorService.java`: Agent planner/researcher/writer/reviewer/publisher workflow.
- `AgentTaskApplicationService.java`: Agent task creation, detail, list, cancel, retry, SSE snapshots.

## Persistence checklist

When changing stored data, inspect all relevant layers:

- entity class
- mapper interface
- mapper XML under `backend/src/main/resources/mapper/`
- SQL bootstrap: `sql/blog_mysql_init.sql`
- schema initializer: `UserSchemaInitializer.java`, `RagSchemaInitializer.java`, or Agent schema initializer if present
- service cache invalidation

## Security expectations

- `/api/agent/**` requires admin role.
- `/api/public/rag/**` requires authenticated users except public search endpoints as configured in `SecurityConfig.java`.
- Comments and uploads remain protected by auth/admin rules.

## Cache-sensitive areas

- Cache names: `backend/src/main/java/com/hejulian/blog/common/CacheNames.java`
- Redis wiring: `backend/src/main/java/com/hejulian/blog/config/RedisConfig.java`
- Public post writes should evict homepage/post-list caches.
- RAG/Ask/Agent history writes should evict RAG session/history caches.
- Qwen config GET should remain read-mostly; capability refresh belongs in save/update.

## Validation

If local Maven exists, use Maven package with tests skipped when appropriate.
If local Maven is unavailable, use `docker compose build backend` from repo root.