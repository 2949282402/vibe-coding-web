---
name: hejulian-web-editing
description: 用于修改、调试、评审或扩展 E:\coding\hejulian-web 仓库，包括其 Vue 前端、Spring Boot 后端、RAG/Ask/Agent 知识页、Agent 编排、管理后台、MyBatis 持久层、Docker/nginx 部署、日志以及静态 /resume 页面。仅在处理该仓库时使用。
---

# Hejulian Web 项目编辑指南

仅对 `E:\coding\hejulian-web` 使用这个 skill。
把它当作项目地图和分流导航，而不是整仓代码转储。

## 核心流程

1. 先判断任务所属区域：前端、后端、RAG/Ask、Agent、部署、简历页或代码评审。
2. 只读取 `references/` 下与该区域匹配的文档。
3. 如果是跨层改动，再按下面的说明显式补读第二份引用文档。
4. 在动手修改前，沿着最小闭环追踪：UI/API -> service -> 持久层 -> 缓存 -> 部署。
5. 保持修改聚焦，不碰无关用户改动。

## 项目结构快照

- 仓库根目录重点包括：`frontend/`、`backend/`、`llm-bridge/`、`sql/`、`deploy/`、`docs/`、`logs/`、`todo/`、`.agents/`、`docker-compose.yml`。
- 前端是 `Vue 3 + Vite + Vue Router + Pinia + Element Plus`，核心源码在 `frontend/src/`，按 `api/`、`layouts/`、`router/`、`stores/`、`views/`、`components/`、`styles/`、`utils/`、`assets/` 分层。
- 后端是 `Spring Boot 3 + MyBatis + Spring Security JWT + Redis + MySQL`，业务主体位于 `com.hejulian.blog`，并内置 `rag/` 与 `agent/` 领域模块。
- RAG、Ask、Agent 运行在同一个后端应用里，不是独立服务。
- `/knowledge` 支持三种响应模式：`RAG`、`Ask`、仅管理员可见的 `Agent`。
- 用户级 Qwen API Key、模型和联网能力配置通过账号配置保存。
- Agent 编排代码位于 `backend/src/main/java/com/hejulian/blog/agent/`，通过 `/api/agent/**` 暴露。
- `llm-bridge/` 是可选的独立 Python 桥接服务，对应 `docker compose` 里的可选 profile。
- 静态 `/resume` 由 nginx 从 `frontend/public/resume/index.html` 提供，不经过 Vue Router。

## 引用文档分流

### 前端 UI、路由、状态、API 客户端

读取 `references/frontend.md`。
如果涉及 `/knowledge`、聊天模式、来源区或流式响应，再补读 `references/rag.md`。

### 后端 API、服务、鉴权、持久层、表结构

读取 `references/backend.md`。
如果涉及聊天/检索/模型行为，再补读 `references/rag.md`；如果涉及运行时接线或容器行为，再补读 `references/deployment.md`。

### `/knowledge`、RAG、Ask、历史、来源、反馈、SSE

优先读取 `references/rag.md`。
改 UI 时再补读 `references/frontend.md`，改 API/service/持久层契约时再补读 `references/backend.md`。

### Agent 写作/发布流程、工具、轨迹、运维后台

优先读取 `references/agent.md`。
涉及共享的 Qwen 运行时或流式行为时补读 `references/rag.md`，涉及持久层或权限时补读 `references/backend.md`。

### Docker、nginx、运行时配置、日志、静态资源投递

读取 `references/deployment.md`。
如果任务与 `/resume` 相关，再补读 `references/resume.md`；如果与 SSE 缓冲相关，再补读 `references/rag.md`。

### 静态简历页

读取 `references/resume.md`。
不要把 `/resume` 当成 Vue 路由页面。

### 代码评审

先读取 `references/review.md`，再读取对应改动区域的引用文档。

## 跨层约束

- 仓库内用户可见说明优先使用中文；如果某处本来同时维护中英文文案，改动时同步两套分支。
- 项目文件和 skill 文件都必须按 UTF-8 处理。
- 在 PowerShell 里读写文件时显式指定 UTF-8，避免默认 ANSI、GBK 或依赖本机区域设置。
- 如果要脚本化写入，优先使用 UTF-8 无 BOM；不要把已有 UTF-8 文件改写成其他编码。
- 不要批量扫全仓；优先走引用文档指向的最小文件集。
- 不要回滚无关工作区改动；默认当前 worktree 可能是脏的。
- 涉及表结构变更时，检查实体、Mapper 接口、Mapper XML、SQL 初始化脚本、Schema Initializer，以及上层服务的缓存失效。
- 涉及公开内容、RAG 会话、历史记录、Qwen 配置或 Agent 任务写入时，检查 Redis 缓存是否同步失效。
- 遇到 SSE 问题时，同时检查后端 `SseEmitter`、前端 `fetch` 流解析，以及 nginx 的 `proxy_buffering off`。
- 修改 Docker 前端 nginx 配置时，要改 `frontend/nginx/default.conf.template`；只改 `default.conf` 不会进入容器镜像。

## 建议验证

- 前端改动：在 `frontend/` 下运行 `npm run build`。
- 后端改动：优先本地 Maven；没有本地 Maven 时，从仓库根目录执行 `docker compose build backend`。
- 需要整栈刷新时：执行 `docker compose up -d --build`。
- 除非用户要求，或改动明显影响编译/运行，否则不要主动跑特别重的验证。
