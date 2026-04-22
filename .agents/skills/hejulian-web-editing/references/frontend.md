# Frontend Reference

Read this file only for frontend-facing tasks.

## Frontend stack

- Vue 3 + Vite
- Vue Router
- Pinia
- Element Plus
- shared global styling in `frontend/src/styles/global.css`

## Frontend structure

### App shell

- app entry: `frontend/src/main.js`
- root app: `frontend/src/App.vue`
- router: `frontend/src/router/index.js`
- public layout: `frontend/src/layouts/MainLayout.vue`
- admin layout: `frontend/src/layouts/AdminLayout.vue`

### Stores

- auth store: `frontend/src/stores/auth.js`
- locale, theme, and preference store: `frontend/src/stores/preferences.js`

### API clients

- public and RAG APIs: `frontend/src/api/blog.js`
- admin APIs: `frontend/src/api/admin.js`
- auth and Qwen config APIs: `frontend/src/api/auth.js`
- base HTTP wrapper: `frontend/src/api/http.js`

### Shared components and helpers

- theme or locale controls: `frontend/src/components/AppControls.vue`
- post summary card: `frontend/src/components/PostCard.vue`
- markdown rendering and sanitization: `frontend/src/utils/markdown.js`

## Route map

### Public routes

- `/`: `frontend/src/views/HomeView.vue`
- `/posts/:slug`: `frontend/src/views/PostDetailView.vue`
- `/archives`: `frontend/src/views/ArchiveView.vue`
- `/categories`: `frontend/src/views/CategoriesView.vue`
- `/knowledge`: `frontend/src/views/KnowledgeView.vue`
- `/login`: `frontend/src/views/LoginView.vue`

### Admin routes

- `/admin/dashboard`: `frontend/src/views/admin/DashboardView.vue`
- `/admin/posts`: `frontend/src/views/admin/PostManageView.vue`
- `/admin/posts/new`: `frontend/src/views/admin/PostEditorView.vue`
- `/admin/posts/:id/edit`: `frontend/src/views/admin/PostEditorView.vue`
- `/admin/taxonomies`: `frontend/src/views/admin/TaxonomyManageView.vue`
- `/admin/comments`: `frontend/src/views/admin/CommentManageView.vue`
- `/admin/rag-feedback`: `frontend/src/views/admin/RagFeedbackManageView.vue`

## Page responsibilities

### Public pages

- `HomeView.vue`: hero area, quick retrieval search, featured posts, latest posts, categories, tags, CTA into `/knowledge`
- `PostDetailView.vue`: article rendering, article metadata, comment entry
- `ArchiveView.vue`: filtered article browsing
- `CategoriesView.vue`: category and tag entry page
- `LoginView.vue`: login/register switching and redirect handling

### Admin pages

- `DashboardView.vue`: summary stats and admin overview
- `PostManageView.vue`: article list, status actions, navigation into editor
- `PostEditorView.vue`: article create/edit, taxonomy selection, markdown editing, uploads
- `TaxonomyManageView.vue`: categories and tags CRUD
- `CommentManageView.vue`: comment moderation
- `RagFeedbackManageView.vue`: RAG feedback list, filtering, CSV export, session jump links

## Key frontend behavior

### Layout and routing

- public pages are nested under `MainLayout.vue`
- `/login` is standalone
- `/admin/**` is nested under `AdminLayout.vue`
- `/knowledge` is immersive and hides the footer
- route guards in `frontend/src/router/index.js` enforce auth and admin access

### Auth and profile flow

- auth state and current user role live in `frontend/src/stores/auth.js`
- login redirects use `redirect` query handling
- admin checks depend on auth store state, not just route naming

### Locale and visual mode

- this repo supports bilingual copy in multiple pages
- preferences store also handles theme or related UI preferences
- when changing visible text, check both Chinese and English branches where they exist

## Common edit entry points

- navigation or shell behavior: `frontend/src/layouts/MainLayout.vue`, `frontend/src/router/index.js`
- admin shell behavior: `frontend/src/layouts/AdminLayout.vue`
- public content data loading: `frontend/src/api/blog.js`, public pages
- admin data loading: `frontend/src/api/admin.js`, admin pages
- auth or Qwen config UI: `frontend/src/api/auth.js`, `frontend/src/stores/auth.js`, `frontend/src/views/LoginView.vue`, `frontend/src/views/KnowledgeView.vue`

## Validation

Preferred validation for frontend changes:

- run `npm run build` in `frontend/`

If the task touches `/knowledge`, also read `references/rag.md`.
If the task touches `/resume`, read `references/resume.md` instead of assuming it is a Vue page.
