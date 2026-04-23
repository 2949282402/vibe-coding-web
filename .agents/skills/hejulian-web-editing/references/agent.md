# Agent Reference

Use for the admin-only Agent workflow: writing/publishing posts, task history, tool traces, memory, SSE progress, Agent ops pages, and `/api/agent/**`.

## Product behavior

- Agent mode is available only to admins.
- Frontend entry is `/knowledge` with chat mode `Agent`.
- Agent can use search scopes `LOCAL_ONLY`, `WEB_ONLY`, and `LOCAL_AND_WEB`.
- Agent writes/publishes posts through backend admin publishing services.
- Runtime progress should be visible through SSE snapshots/progress and Execution Trace.
- The final article should appear at the end of the Execution Trace and be returned conversationally.

## Frontend files

- Main user entry: `frontend/src/views/KnowledgeView.vue`
- API wrapper: `frontend/src/api/blog.js`
- Admin ops page: `frontend/src/views/admin/AgentOpsView.vue`
- Tool call admin page: `frontend/src/views/admin/AgentToolCallsView.vue`
- Admin API wrapper when relevant: `frontend/src/api/admin.js`

## Backend package map

Base: `backend/src/main/java/com/hejulian/blog/agent/`

- `controller/`: Agent task, ops, memory/tool/admin endpoints.
- `application/`: orchestration, task app service, tool service, memory, trace.
- `domain/`: enums, tool contracts, domain models.
- `dto/`: Agent DTOs and response contracts.
- `entity/`: Agent task, step, event, tool call, memory entities.
- `infrastructure/tool/`: concrete tools such as site search, chunk search, web search, save note.
- `mapper/`: MyBatis mapper interfaces.
- XML mappers live under `backend/src/main/resources/mapper/`.

## Critical backend chain

- Task API: `AgentTaskController.java`
- Task app service: `AgentTaskApplicationService.java`
- Orchestrator: `AgentOrchestratorService.java`
- Tool service: `AgentToolService.java`
- Trace/event service: `AgentTraceService.java`
- Publishing: `AdminBlogService.java`
- Runtime config: `AuthService.requireRagRuntimeOptions(...)`, `RagRuntimeContextHolder`, `DashScopeModelGateway`

## Normal task lifecycle

1. Frontend creates task via `POST /api/agent/tasks`.
2. Backend inserts `agent_task` as pending.
3. Orchestrator runs async.
4. Frontend opens `/api/agent/tasks/{taskId}/stream`.
5. SSE emits progress/snapshot/done.
6. Orchestrator runs planner, researcher, writer, reviewer, and publisher.
7. Publisher saves article and task note.
8. Backend persists conversation/history and evicts caches.
9. Frontend renders steps/tools/final article in Execution Trace.

## Agent roles and tools

Typical roles:

- Planner: builds plan and reads memory summary.
- Researcher: calls site/chunk/web tools based on search scope.
- Writer: drafts the article.
- Reviewer: cleans and validates the draft.
- Publisher: saves note and publishes post.

Common tools:

- `get_user_memory_summary`
- `search_site_posts`
- `search_knowledge_chunks`
- `web_search`
- `save_task_note`

## Runtime and search rules

- Always ensure user Qwen runtime is available before model generation.
- For `WEB_ONLY`, skip local site/chunk tools and call `web_search`.
- For `LOCAL_AND_WEB`, use local tools and web search.
- For `LOCAL_ONLY`, use local tools only.
- If web search is requested but the selected Qwen model lacks web support, fail clearly.

## Persistence and cache checks

When changing Agent data, inspect:

- Agent entities and mappers.
- `AgentTaskMapper.xml`, step/event/tool/memory mapper XML.
- SQL bootstrap under `sql/blog_mysql_init.sql`.
- Any Agent schema initializer if present.
- RAG history/session cache eviction when Agent writes conversation messages.
- Public homepage/post-list cache eviction when Agent publishes posts.

## SSE and nginx

Agent stream endpoint requires special nginx handling:

- `/api/agent/tasks/{taskId}/stream`
- `proxy_buffering off`
- `proxy_cache off`
- long read/send timeouts
- `X-Accel-Buffering no`

For Docker frontend, update `frontend/nginx/default.conf.template`; `frontend/nginx/default.conf` alone is insufficient.

## Common pitfalls

- Running Agent synchronously from the create/retry request and hiding progress until completion.
- Marking a cancelled task completed after cancellation.
- Publishing a draft that still contains plan/tool/debug text.
- Saving `Draft for:` style placeholders instead of the final article.
- Forgetting to display tool calls and multi-agent steps dynamically in the frontend.
- Forgetting to evict homepage caches after publishing.
- Letting normal users access `/api/agent/**`.

## Validation

- Frontend changes: `npm run build`.
- Backend changes: `docker compose build backend` if Maven is unavailable.
- For runtime issues, inspect dated logs under `logs/YYYY-MM-DD/`.