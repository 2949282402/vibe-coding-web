# Frontend Reference

Use for Vue pages, layouts, routes, stores, API clients, UI state, copy, and client-side interactions.

## Stack and roots

- App entry: `frontend/src/main.js`
- Root app: `frontend/src/App.vue`
- Router: `frontend/src/router/index.js`
- Public layout: `frontend/src/layouts/MainLayout.vue`
- Admin layout: `frontend/src/layouts/AdminLayout.vue`
- Global styles: `frontend/src/styles/global.css`
- API clients: `frontend/src/api/http.js`, `blog.js`, `admin.js`, `auth.js`
- Stores: `frontend/src/stores/auth.js`, `preferences.js`

## Main public routes

- `/`: `frontend/src/views/HomeView.vue`
- `/posts/:slug`: `frontend/src/views/PostDetailView.vue`
- `/archives`: `ArchiveView.vue`
- `/categories`: `CategoriesView.vue`
- `/knowledge`: `KnowledgeView.vue`
- `/login`: `LoginView.vue`

## Main admin routes

- Dashboard: `frontend/src/views/admin/DashboardView.vue`
- Posts: `PostManageView.vue`, `PostEditorView.vue`
- Taxonomy: `TaxonomyManageView.vue`
- Comments: `CommentManageView.vue`
- RAG feedback: `RagFeedbackManageView.vue`
- Agent ops: `AgentOpsView.vue`
- Agent tool calls: `AgentToolCallsView.vue`

## `/knowledge` integration points

Read `references/rag.md` before changing this page.
Key files:

- Page: `frontend/src/views/KnowledgeView.vue`
- Public/RAG/Agent API wrapper: `frontend/src/api/blog.js`
- Markdown and citation rendering: `frontend/src/utils/markdown.js`

Current UI modes:

- `RAG`: retrieval-augmented answer with source/citation behavior.
- `Ask`: pure LLM chat, sends `answerMode: ASK`, hides search-scope picker.
- `Agent`: admin-only writing/publishing workflow using `/api/agent/**` and Agent SSE.

Search scopes used by RAG/Agent:

- `LOCAL_ONLY`
- `WEB_ONLY`
- `LOCAL_AND_WEB`

## Auth and guards

- `authStore.isAuthenticated` gates comments/RAG/Ask usage.
- `authStore.isAdmin` gates admin routes and Agent mode visibility.
- Backend security remains authoritative; frontend guards are convenience only.

## Locale/copy rules

- `KnowledgeView.vue` has Chinese and English copy branches.
- Keep all files UTF-8.
- Avoid adding English-only visible text to Chinese UI.

## Validation

Run `npm run build` in `frontend/` after Vue, route, template, API payload, or copy changes.