# 前端参考

用于处理 Vue 页面、布局、路由、状态管理、API 客户端、交互状态和前端文案。

## 技术栈与核心入口

- 应用入口：`frontend/src/main.js`
- 根组件：`frontend/src/App.vue`
- 路由：`frontend/src/router/index.js`
- 公共布局：`frontend/src/layouts/MainLayout.vue`
- 管理端布局：`frontend/src/layouts/AdminLayout.vue`
- 全局样式：`frontend/src/styles/global.css`
- API 客户端：`frontend/src/api/http.js`、`blog.js`、`admin.js`、`auth.js`
- Store：`frontend/src/stores/auth.js`、`preferences.js`
- Markdown / 引用渲染：`frontend/src/utils/markdown.js`
- 结构补充：`frontend/src/` 下还包含 `components/`、`assets/`、`styles/`、`utils/`

## 公共路由

- `/`：`frontend/src/views/HomeView.vue`
- `/knowledge`：`frontend/src/views/KnowledgeView.vue`
- `/posts/:slug`：`frontend/src/views/PostDetailView.vue`
- `/archives`：`frontend/src/views/ArchiveView.vue`
- `/categories`：`frontend/src/views/CategoriesView.vue`
- `/login`：`frontend/src/views/LoginView.vue`

## 管理端路由

- `/admin/dashboard`：`frontend/src/views/admin/DashboardView.vue`
- `/admin/posts`：`PostManageView.vue`
- `/admin/posts/new`、`/admin/posts/:id/edit`：`PostEditorView.vue`
- `/admin/taxonomies`：`TaxonomyManageView.vue`
- `/admin/comments`：`CommentManageView.vue`
- `/admin/rag-feedback`：`RagFeedbackManageView.vue`
- `/admin/agent-drafts`：`AgentDraftReviewView.vue`
- `/admin/agents`：`AgentOpsView.vue`
- `/admin/agent-tool-calls`：`AgentToolCallsView.vue`

## `/knowledge` 关联点

修改这个页面前先读 `references/rag.md`。
关键文件：

- 页面：`frontend/src/views/KnowledgeView.vue`
- 公共 RAG / Ask / Agent API 封装：`frontend/src/api/blog.js`
- Markdown 与引用渲染：`frontend/src/utils/markdown.js`

当前 UI 模式：

- `RAG`：检索增强回答，带来源、引用、搜索范围和历史记录。
- `Ask`：纯大模型对话，发送 `answerMode: ASK`，隐藏搜索范围切换。
- `Agent`：仅管理员可见，走 `/api/agent/**` 与 Agent SSE 流程。

RAG / Agent 使用的搜索范围：

- `LOCAL_ONLY`
- `WEB_ONLY`
- `LOCAL_AND_WEB`

## 鉴权与路由守卫

- `authStore.isAuthenticated` 控制登录后才能使用的能力，例如评论与 RAG / Ask。
- `authStore.isAdmin` 控制管理端路由和 Agent 模式入口可见性。
- 前端守卫只做体验兜底，最终权限以后端 `SecurityConfig` 为准。

## 文案与编码

- `KnowledgeView.vue` 内存在中文与英文文案分支时，改动要同步。
- 所有文件保持 UTF-8。
- 不要往中文 UI 中混入孤立的英文可见文案。

## 验证

改动 Vue、路由、模板、API 载荷或用户可见文案后，在 `frontend/` 下运行 `npm run build`。
