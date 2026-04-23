# RAG, Ask, and `/knowledge` Reference

Use for `/knowledge`, RAG retrieval, pure LLM Ask mode, sources/citations, chat history, replay, feedback, search modes, and SSE.

## User-facing modes

`frontend/src/views/KnowledgeView.vue` exposes three response modes:

- `RAG`: retrieval-enhanced answer. Uses sources, citations, search scopes, history, feedback, replay.
- `Ask`: pure LLM chat. Sends `answerMode: ASK`; backend skips local/web retrieval and returns `mode: ask` with no sources/citations.
- `Agent`: admin-only workflow. Uses `/api/agent/**`; see `references/agent.md`.

## Search scopes

Search scope is separate from response mode and applies to RAG/Agent, not Ask UI:

- `LOCAL_ONLY`: site/RAG local retrieval only.
- `WEB_ONLY`: Qwen web search only.
- `LOCAL_AND_WEB`: local retrieval plus Qwen web search.

## Frontend chain

- Page: `frontend/src/views/KnowledgeView.vue`
- API wrapper: `frontend/src/api/blog.js`
- Markdown/citation renderer: `frontend/src/utils/markdown.js`

Important frontend behavior:

- SSE parser uses `fetch` + `ReadableStream`, not `EventSource`, so Authorization headers can be sent.
- RAG/Ask stream endpoint: `/api/public/rag/ask/stream`.
- Agent stream endpoint: `/api/agent/tasks/{taskId}/stream`.
- The source rail is meaningful for RAG results; Ask results should have no sources.
- Keep session list/history cache updates aligned with backend writes.

## Backend chain

- Controller: `backend/src/main/java/com/hejulian/blog/controller/RagController.java`
- DTO: `backend/src/main/java/com/hejulian/blog/dto/RagDtos.java`
- Service: `backend/src/main/java/com/hejulian/blog/rag/application/RagApplicationService.java`
- Runtime holder: `RagRuntimeContextHolder.java`
- Qwen gateway: `DashScopeModelGateway.java`
- Prompt/citation helpers: `RagPromptService.java`, `CitationGuardService.java`
- Retrieval: `KnowledgeRetrievalService.java`, `MybatisRagKnowledgeBaseRepository.java`, `QdrantVectorStore.java`
- Persistence mappers: `RagChatSessionMapper`, `RagChatMessageMapper`, `RagChunkMapper` and their XML files.

## DTO contracts to preserve

- `RagDtos.AskRequest`: `question`, `topK`, `sessionId`, `searchMode`, `answerMode`.
- `answerMode: ASK` means pure model chat.
- `RagDtos.AskResponse`: contains `sessionId`, `question`, `answer`, `mode`, `sources`, `history`, `strictCitation`, `searchMode`.
- Chat message `mode` can include `retrieval`, `llm`, `ask`, or `agent`.

## SSE contract

Backend sends `StreamEvent` with:

- `type: meta`: initial state and empty answer.
- `type: delta`: streamed text chunk.
- `type: done`: final `AskResponse`.
- `type: error`: failure message.

If streaming appears delayed, check all three layers:

- Backend `SseEmitter` sends events promptly.
- Frontend parser handles event framing and updates `pendingTurn`/`result`.
- Nginx has `proxy_buffering off` and `X-Accel-Buffering no` for stream endpoints.

## History, replay, feedback

Relevant endpoints:

- `POST /api/public/rag/ask`
- `POST /api/public/rag/ask/stream`
- `POST /api/public/rag/replay`
- `GET /api/public/rag/history`
- `GET /api/public/rag/sessions`
- rename/delete/restore/purge session endpoints
- `POST /api/public/rag/feedback`

When editing these flows, check cache invalidation for RAG history and session list.

## Common pitfalls

- Treating Ask as RAG with zero sources. Ask should bypass retrieval intentionally.
- Requiring citation validation for Ask output. Ask should return `strictCitation=false`.
- Hiding the chat-mode switch in Ask mode; only hide the search-scope switch.
- Adding web search through non-Qwen/legacy paths.
- Changing response shapes without updating `KnowledgeView.vue` and `frontend/src/api/blog.js`.

## Validation

- Frontend: `npm run build`.
- Backend: Maven package or `docker compose build backend`.
- For SSE buffering changes, rebuild/recreate frontend container because nginx template is baked into the image.