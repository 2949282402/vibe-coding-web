# RAG Reference

Read this file only for `/knowledge` or backend RAG tasks.

## Scope

Use this reference for:

- `/knowledge` UI behavior
- SSE chat streaming
- retrieval logic
- sources and citations
- history restore
- replay, feedback, rename, delete, restore, purge
- search mode behavior

## Core chain

Always trace the full chain before editing:

### Frontend

- `frontend/src/views/KnowledgeView.vue`
- `frontend/src/api/blog.js`
- `frontend/src/utils/markdown.js`

### Backend entry

- `backend/src/main/java/com/hejulian/blog/controller/RagController.java`
- `backend/src/main/java/com/hejulian/blog/rag/application/RagApplicationService.java`

### Backend supporting modules

- runtime holder: `backend/src/main/java/com/hejulian/blog/rag/application/RagRuntimeContextHolder.java`
- indexing flow: `backend/src/main/java/com/hejulian/blog/rag/application/RagIndexingApplicationService.java`
- config: `backend/src/main/java/com/hejulian/blog/rag/config/RagProperties.java`
- retrieval: `backend/src/main/java/com/hejulian/blog/rag/domain/service/KnowledgeRetrievalService.java`
- text processing: `KnowledgeTextProcessor.java`
- prompt construction: `RagPromptService.java`
- citation handling: `CitationGuardService.java`
- model gateway: `backend/src/main/java/com/hejulian/blog/rag/infrastructure/client/DashScopeModelGateway.java`
- repository: `backend/src/main/java/com/hejulian/blog/rag/infrastructure/persistence/MybatisRagKnowledgeBaseRepository.java`
- schema initializer: `backend/src/main/java/com/hejulian/blog/rag/infrastructure/persistence/RagSchemaInitializer.java`
- vector store: `backend/src/main/java/com/hejulian/blog/rag/infrastructure/vector/QdrantVectorStore.java`
- optional bridge: `backend/src/main/java/com/hejulian/blog/service/PythonBridgeClient.java`

### Persistence

- mapper interfaces: `backend/src/main/java/com/hejulian/blog/mapper/RagChunkMapper.java`, `RagChatSessionMapper.java`, `RagChatMessageMapper.java`
- mapper XML: `backend/src/main/resources/mapper/RagChatSessionMapper.xml`, `RagChatMessageMapper.xml`, `RagChunkMapper.xml`

## Frontend knowledge structure

`frontend/src/views/KnowledgeView.vue` is the main integration point.
It contains multiple tightly coupled areas:

- left session rail
- middle conversation timeline
- right source rail
- composer
- search mode selection
- citation jump behavior
- history restore logic
- feedback actions
- source grouping and expansion behavior

Avoid broad rewrites.
Patch only the relevant block.

## Frontend API behavior

`frontend/src/api/blog.js` includes:

- normal HTTP RAG APIs
- public retrieval-only search API
- SSE `fetch` handling for `/api/public/rag/ask/stream`

If the backend response shape changes, verify this parser and the `KnowledgeView.vue` consumer still match.

## Current product assumptions

These behaviors should be preserved unless the user explicitly asks to change them:

- chat input stays fixed near the bottom
- left rail, center timeline, and right rail scroll independently
- latest answer uses the right source rail as the main source display
- older assistant messages may show compact inline source chips
- citation clicks still activate the correct source owner and jump to the correct source
- restored history includes enough `sources` data for old citations to keep working
- search mode options are only `LOCAL_ONLY` and `LOCAL_AND_WEB`
- web search uses the Qwen compatible path with `enable_search=true`

## Session and feedback lifecycle

Relevant endpoints and flows include:

- ask
- ask stream
- replay
- session list
- history
- rename
- delete
- restore
- purge
- feedback

If the task touches any of these, trace from:

- controller
- application service
- mapper interface
- mapper XML
- cache invalidation

## Search behavior

Expected modes:

- `LOCAL_ONLY`: retrieval only
- `LOCAL_AND_WEB`: retrieval plus Qwen-compatible web search

Do not accidentally reintroduce removed DuckDuckGo or legacy web-search paths.

## Qwen configuration and runtime options

These areas are closely related:

- `backend/src/main/java/com/hejulian/blog/service/AuthService.java`
- `backend/src/main/java/com/hejulian/blog/rag/infrastructure/client/DashScopeModelGateway.java`
- `backend/src/main/java/com/hejulian/blog/rag/application/RagRuntimeContextHolder.java`

Current expectations:

- `GET /api/auth/qwen-config` should be read-mostly
- refresh should not re-probe all remote model capabilities
- `POST /api/auth/qwen-config` is the right place to refresh capability detection

## Cache-sensitive areas

Inspect:

- `backend/src/main/java/com/hejulian/blog/common/CacheNames.java`
- `backend/src/main/java/com/hejulian/blog/config/RedisConfig.java`

Writes involving history or sessions should evict related caches.

## Validation

After RAG changes, verify what applies:

- streaming contract still matches frontend parsing
- history restore still reconstructs citations and sources
- source ownership and citation jumps still work
- session lifecycle operations still invalidate the right caches

If the task also changes UI layout, read `references/frontend.md`.
If the task also changes service wiring or persistence, read `references/backend.md`.
