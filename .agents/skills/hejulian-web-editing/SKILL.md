---
name: hejulian-web-editing
description: Use this skill only when modifying, debugging, reviewing, or extending the hejulian-web repository at E:\coding\hejulian-web, especially its Vue frontend, Spring Boot backend, and RAG chat features.
---

# Hejulian Web Editing

Only use this skill when the task is about the `hejulian-web` project in `E:\coding\hejulian-web`.
If the task is for another repo or is unrelated to this codebase, do not use this skill.

## When this skill should trigger

Use it for requests such as:

- modify the blog frontend or backend in this repo
- adjust the `/knowledge` RAG chat page
- add or fix admin, article, taxonomy, or feedback features in this repo
- change RAG history, sources, search mode, feedback, or retrieval behavior
- review a change in this repo with repo-specific context

## Project snapshot

- Docker-first personal blog project with `frontend/` and `backend/`
- Frontend: Vue 3 + Vite
- Backend: Spring Boot 3 + MyBatis + JWT
- RAG logic lives inside the backend, not as a separate service
- Optional local model bridge exists in `llm-bridge/`
- Redis is enabled through Spring Cache and is now used for public content caches, RAG session or history caches, and cached Qwen model capability results
- User login is required for comments and RAG chat, but public article browsing and homepage retrieval remain public
- Per-user Qwen API Key, selected model, and web-search support state are stored on the user record
- Current web search path uses Qwen compatible chat completions with `enable_search=true`; do not switch back to the older separate web-search service path unless explicitly requested

Key paths:

- `frontend/src/views/KnowledgeView.vue`
- `frontend/src/api/blog.js`
- `frontend/src/api/auth.js`
- `frontend/src/stores/auth.js`
- `frontend/src/router/index.js`
- `frontend/src/layouts/MainLayout.vue`
- `backend/src/main/java/com/hejulian/blog/rag/`
- `backend/src/main/java/com/hejulian/blog/service/AuthService.java`
- `backend/src/main/java/com/hejulian/blog/common/CacheNames.java`
- `backend/src/main/java/com/hejulian/blog/config/RedisConfig.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/hejulian/blog/rag/infrastructure/persistence/RagSchemaInitializer.java`
- `sql/blog_mysql_init.sql`

## Feature module map

Use this section to quickly locate where a feature lives before editing.

### Frontend page modules

- Public site shell: `frontend/src/layouts/MainLayout.vue`
- Admin shell: `frontend/src/layouts/AdminLayout.vue`
- Router entry for all pages: `frontend/src/router/index.js`
- Home page: `frontend/src/views/HomeView.vue`
- Article detail page: `frontend/src/views/PostDetailView.vue`
- Archive page: `frontend/src/views/ArchiveView.vue`
- Category or tag listing page entry: `frontend/src/views/CategoriesView.vue`
- Login page: `frontend/src/views/LoginView.vue`
- Knowledge chat page: `frontend/src/views/KnowledgeView.vue`
- User auth and profile actions currently surface through `frontend/src/layouts/MainLayout.vue` and `frontend/src/views/LoginView.vue`

### Frontend admin modules

- Admin dashboard page: `frontend/src/views/admin/DashboardView.vue`
- Article list management: `frontend/src/views/admin/PostManageView.vue`
- Article create and edit page: `frontend/src/views/admin/PostEditorView.vue`
- Taxonomy or tag management page: `frontend/src/views/admin/TaxonomyManageView.vue`
- Comment management page: `frontend/src/views/admin/CommentManageView.vue`
- RAG feedback statistics and management page: `frontend/src/views/admin/RagFeedbackManageView.vue`

### Frontend shared modules

- Public and knowledge API requests: `frontend/src/api/blog.js`
- Admin API requests: `frontend/src/api/admin.js`
- Auth API requests: `frontend/src/api/auth.js`
- HTTP client base wrapper: `frontend/src/api/http.js`
- Markdown and citation rendering: `frontend/src/utils/markdown.js`
- Auth state store: `frontend/src/stores/auth.js`
- Locale and preference state: `frontend/src/stores/preferences.js`
- Shared UI control component: `frontend/src/components/AppControls.vue`
- Post card component: `frontend/src/components/PostCard.vue`
- Global style entry: `frontend/src/styles/global.css`

### Backend public and auth modules

- Public blog controller: `backend/src/main/java/com/hejulian/blog/controller/PublicBlogController.java`
- Auth controller: `backend/src/main/java/com/hejulian/blog/controller/AuthController.java`
- Public blog service: `backend/src/main/java/com/hejulian/blog/service/PublicBlogService.java`
- Auth service: `backend/src/main/java/com/hejulian/blog/service/AuthService.java`
- JWT and permission config: `backend/src/main/java/com/hejulian/blog/config/SecurityConfig.java`
- JWT filter and token logic: `backend/src/main/java/com/hejulian/blog/security/`
- Redis cache config: `backend/src/main/java/com/hejulian/blog/config/RedisConfig.java`
- Shared cache names: `backend/src/main/java/com/hejulian/blog/common/CacheNames.java`

### Backend admin modules

- Admin dashboard controller: `backend/src/main/java/com/hejulian/blog/controller/admin/AdminDashboardController.java`
- Admin article controller: `backend/src/main/java/com/hejulian/blog/controller/admin/AdminPostController.java`
- Admin taxonomy controller: `backend/src/main/java/com/hejulian/blog/controller/admin/AdminTaxonomyController.java`
- Admin comment controller: `backend/src/main/java/com/hejulian/blog/controller/admin/AdminCommentController.java`
- Admin RAG feedback controller: `backend/src/main/java/com/hejulian/blog/controller/admin/AdminRagFeedbackController.java`
- Admin upload controller: `backend/src/main/java/com/hejulian/blog/controller/admin/AdminUploadController.java`
- Main admin business service: `backend/src/main/java/com/hejulian/blog/service/AdminBlogService.java`
- Upload storage service: `backend/src/main/java/com/hejulian/blog/service/UploadStorageService.java`

### Backend RAG modules

- RAG API entry: `backend/src/main/java/com/hejulian/blog/controller/RagController.java`
- RAG application orchestration: `backend/src/main/java/com/hejulian/blog/rag/application/RagApplicationService.java`
- RAG indexing application flow: `backend/src/main/java/com/hejulian/blog/rag/application/RagIndexingApplicationService.java`
- RAG domain services: `backend/src/main/java/com/hejulian/blog/rag/domain/service/`
- RAG domain models: `backend/src/main/java/com/hejulian/blog/rag/domain/model/`
- RAG ports and abstractions: `backend/src/main/java/com/hejulian/blog/rag/domain/port/`
- DashScope or Qwen gateway: `backend/src/main/java/com/hejulian/blog/rag/infrastructure/client/DashScopeModelGateway.java`
- MyBatis-based RAG persistence: `backend/src/main/java/com/hejulian/blog/rag/infrastructure/persistence/MybatisRagKnowledgeBaseRepository.java`
- RAG schema initializer: `backend/src/main/java/com/hejulian/blog/rag/infrastructure/persistence/RagSchemaInitializer.java`
- Qdrant vector store integration: `backend/src/main/java/com/hejulian/blog/rag/infrastructure/vector/QdrantVectorStore.java`
- RAG runtime config: `backend/src/main/java/com/hejulian/blog/rag/config/RagProperties.java`
- Chat session and history cache orchestration currently lives in `backend/src/main/java/com/hejulian/blog/rag/application/RagApplicationService.java`

### Backend persistence modules

- Post mapper: `backend/src/main/java/com/hejulian/blog/mapper/PostMapper.java`
- Category mapper: `backend/src/main/java/com/hejulian/blog/mapper/CategoryMapper.java`
- Tag mapper: `backend/src/main/java/com/hejulian/blog/mapper/TagMapper.java`
- Comment mapper: `backend/src/main/java/com/hejulian/blog/mapper/CommentMapper.java`
- User mapper: `backend/src/main/java/com/hejulian/blog/mapper/UserAccountMapper.java`
- RAG chunk mapper: `backend/src/main/java/com/hejulian/blog/mapper/RagChunkMapper.java`
- RAG chat session mapper: `backend/src/main/java/com/hejulian/blog/mapper/RagChatSessionMapper.java`
- RAG chat message mapper: `backend/src/main/java/com/hejulian/blog/mapper/RagChatMessageMapper.java`
- MyBatis XML mappings: `backend/src/main/resources/mapper/`
- Base SQL initialization: `sql/blog_mysql_init.sql`

### High-frequency edit entry points by feature

- Article publishing, editing, cover upload, body editor: `frontend/src/views/admin/PostEditorView.vue`, `backend/src/main/java/com/hejulian/blog/controller/admin/AdminPostController.java`, `backend/src/main/java/com/hejulian/blog/service/AdminBlogService.java`
- Article list and status changes: `frontend/src/views/admin/PostManageView.vue`, `backend/src/main/java/com/hejulian/blog/controller/admin/AdminPostController.java`
- Category or tag management: `frontend/src/views/admin/TaxonomyManageView.vue`, `backend/src/main/java/com/hejulian/blog/controller/admin/AdminTaxonomyController.java`
- Comment moderation: `frontend/src/views/admin/CommentManageView.vue`, `backend/src/main/java/com/hejulian/blog/controller/admin/AdminCommentController.java`
- Login and permission issues: `frontend/src/views/LoginView.vue`, `frontend/src/stores/auth.js`, `backend/src/main/java/com/hejulian/blog/controller/AuthController.java`, `backend/src/main/java/com/hejulian/blog/security/`
- User Qwen API Key, model, and web-search capability issues: `frontend/src/views/KnowledgeView.vue`, `frontend/src/api/auth.js`, `backend/src/main/java/com/hejulian/blog/service/AuthService.java`, `backend/src/main/java/com/hejulian/blog/rag/infrastructure/client/DashScopeModelGateway.java`
- Public article display issues: `frontend/src/views/HomeView.vue`, `frontend/src/views/PostDetailView.vue`, `frontend/src/views/ArchiveView.vue`, `backend/src/main/java/com/hejulian/blog/controller/PublicBlogController.java`
- Knowledge chat UI issues: `frontend/src/views/KnowledgeView.vue`, `frontend/src/api/blog.js`, `frontend/src/utils/markdown.js`
- RAG answer generation, sources, feedback, history, deletion, search mode: `backend/src/main/java/com/hejulian/blog/controller/RagController.java`, `backend/src/main/java/com/hejulian/blog/rag/application/RagApplicationService.java`, `backend/src/main/resources/mapper/RagChatSessionMapper.xml`, `backend/src/main/resources/mapper/RagChatMessageMapper.xml`
- RAG cache and refresh performance issues: `backend/src/main/java/com/hejulian/blog/common/CacheNames.java`, `backend/src/main/java/com/hejulian/blog/config/RedisConfig.java`, `backend/src/main/java/com/hejulian/blog/service/AuthService.java`, `backend/src/main/java/com/hejulian/blog/rag/application/RagApplicationService.java`
- Feedback admin panel issues: `frontend/src/views/admin/RagFeedbackManageView.vue`, `backend/src/main/java/com/hejulian/blog/controller/admin/AdminRagFeedbackController.java`
- Image upload issues: `backend/src/main/java/com/hejulian/blog/controller/admin/AdminUploadController.java`, `backend/src/main/java/com/hejulian/blog/service/UploadStorageService.java`, plus the calling frontend page

## Mandatory working style for this repo

Before changing code:

1. Give a short numbered task list first.
2. Follow that list strictly.
3. Do not bundle unrelated feature changes.

While working:

- Prefer Chinese in user-facing communication.
- If the feature has Chinese mode, avoid leaving visible English in the Chinese UI.
- Assume the git worktree may already be dirty; never revert unrelated changes.
- Keep edits small and targeted.
- Preserve existing style and behavior unless the user asked to change them.

## Repo-specific guardrails

### 1. RAG chat is fragile; preserve the end-to-end chain

When touching RAG chat, check the full chain:

- backend streaming or answer contract
- frontend request parser in `frontend/src/api/blog.js`
- chat timeline rendering in `frontend/src/views/KnowledgeView.vue`
- source persistence and history restoration

If you change one part of the chain, verify the other parts still match.

### 2. KnowledgeView.vue is the main integration point

`frontend/src/views/KnowledgeView.vue` is large and holds multiple coupled behaviors:

- left session rail
- middle conversation timeline
- right reference/source rail
- composer behavior
- follow-up question UI
- source expansion and citation jumping
- history restore behavior

When editing it, avoid broad rewrites. Patch only the relevant block and keep the rest stable.

### 3. Current RAG UX assumptions to preserve unless requested otherwise

The current project state expects:

- chat input area fixed near the bottom of the page
- left rail, center conversation, and right rail each scroll independently
- latest answer uses the right reference panel as the main source display
- older assistant messages may show compact inline sources at the end of the reply
- citation clicks should still activate the correct source owner and jump to the matching source
- history records should restore message `sources` so old citations still work
- search mode offers only `LOCAL_ONLY` and `LOCAL_AND_WEB`
- web search uses the Qwen compatible chat path with `enable_search=true`; do not reintroduce the old DuckDuckGo scraping logic or the separate old web-search service path unless explicitly asked

### 4. Feedback features have product requirements

If modifying answer feedback:

- keep the disclosure that admins can review the feedback together with related chat history
- preserve admin-side feedback management behavior
- do not silently remove copy/export helpers if present

### 5. Locale handling matters

This repo already has bilingual handling in several places.
When touching visible copy:

- update both Chinese and English text when both exist
- Chinese mode should read naturally and avoid stray English terms
- avoid adding new visible text in English only

### 6. Persistence changes may live in more than one place

If a change affects schema or stored RAG data, inspect whether it belongs in:

- `sql/blog_mysql_init.sql`
- `RagSchemaInitializer.java`
- MyBatis mapper XML files
- repository or service code

Do not assume one file is enough.

### 7. Refresh performance assumptions now matter

The current project state expects:

- opening `/knowledge` should not trigger a full remote model-capability probe on every refresh
- `GET /api/auth/qwen-config` should prefer saved state or cached capability results
- `POST /api/auth/qwen-config` is the right place to probe and refresh available models when the user updates a key
- RAG session lists and chat history should use Redis-backed Spring Cache where possible
- any write to chat history, replay, feedback, rename, delete, restore, or purge must evict the related caches

## Practical workflow

### Frontend changes

Usually inspect:

- `frontend/src/views/KnowledgeView.vue`
- `frontend/src/api/blog.js`
- related router or layout files if navigation or shell behavior changes

Preferred validation:

- run `npm run build` in `frontend/`

This is the fastest reliable check for this repo and has been the default validation path for many UI changes here.

### Backend changes

Usually inspect:

- controller
- application service
- domain port or repository
- mapper interface
- mapper XML
- persistence initializer if schema changed
- cache names or Redis config if the change affects refresh cost or repeated reads

If changing chat history, sources, feedback, or session deletion flows, trace the data from controller to mapper XML instead of patching only one layer.

Preferred validation:

- run targeted backend tests or package commands when the environment supports Maven
- if Maven is unavailable locally, say that clearly and still validate all reachable frontend or static parts

### Review requests

If the user asks for a review, focus on:

- regressions in citation jumping and source ownership
- breakage in history restore
- mismatch between backend payload and frontend expectations
- locale regressions
- layout regressions on the three-column knowledge page

## Known implementation hotspots

### RAG source and history behavior

Recent work in this repo established these expectations:

- assistant messages carry `sources`
- history APIs should return enough source data for restored messages
- right rail source cards are grouped by on-site and web sources
- both groups default to four records, then expand on demand
- older answers can show compact inline source chips at the end of the message

If a new change breaks any of these, treat it as a likely regression.

### Search behavior

- `LOCAL_ONLY`: station or knowledge-base retrieval only
- `LOCAL_AND_WEB`: station retrieval plus Qwen compatible chat completions with `enable_search=true`

Do not add back removed DuckDuckGo code paths by accident.

### Qwen configuration behavior

- `GET /api/auth/qwen-config` should be treated as a read-mostly endpoint
- avoid re-probing all candidate models on every page refresh
- capability probing is expensive because it may call multiple remote models
- prefer caching model capability results by API Key and using the saved selected model for page restore
- if a user reports that the model list is wrong, inspect `backend/src/main/java/com/hejulian/blog/service/AuthService.java` and `backend/src/main/java/com/hejulian/blog/rag/infrastructure/client/DashScopeModelGateway.java` together

### Composer behavior

Current behavior is expected to remain consistent with the implemented keyboard logic.
Before changing send shortcuts, confirm the actual code path in `handleComposerKeydown`.

## Validation checklist

After finishing a change, check what applies:

- Does the modified feature still match the task list given at the start?
- If `KnowledgeView.vue` changed, does the page still keep one-screen layout with independent scrolling areas?
- If citations or sources changed, can the user still open history and jump to references?
- If locale text changed, were both Chinese and English copies updated?
- If frontend code changed, did `frontend` build successfully?
- If backend payload shape changed, did you verify the matching frontend consumer?
- If backend refresh behavior changed, did you verify which requests still hit remote model APIs and which now use Redis or saved state?

## Response style for this repo

When reporting results:

- start with what changed and where
- mention only the relevant files
- state validation clearly
- suggest next steps only if they are natural and directly useful
