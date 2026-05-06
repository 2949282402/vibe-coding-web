# Agent 参考

用于处理仅管理员可见的 Agent 工作流：写作与发布文章、任务历史、工具调用轨迹、记忆、SSE 进度、Agent 运维页面以及 `/api/agent/**`。

## 产品行为

- Agent 模式只对管理员开放。
- 前端主入口在 `/knowledge` 的 `Agent` 聊天模式。
- Agent 可以使用 `LOCAL_ONLY`、`WEB_ONLY`、`LOCAL_AND_WEB` 三种搜索范围。
- Agent 通过后端后台发布服务写入并发布文章。
- 运行进度需要通过 SSE 快照 / 进度事件和 Execution Trace 可见。
- 最终文章既要出现在 Execution Trace 中，也要以对话结果形式返回。

## 前端文件

- 用户主入口：`frontend/src/views/KnowledgeView.vue`
- API 封装：`frontend/src/api/blog.js`
- 管理端运维页：`frontend/src/views/admin/AgentOpsView.vue`
- 管理端工具调用页：`frontend/src/views/admin/AgentToolCallsView.vue`
- 管理端草稿审核页：`frontend/src/views/admin/AgentDraftReviewView.vue`
- 需要时配合后台 API 封装：`frontend/src/api/admin.js`

## 后端包结构

基础路径：`backend/src/main/java/com/hejulian/blog/agent/`

- `controller/`：任务、记忆、工具相关接口。
- `controller/admin/`：Agent 管理后台接口。
- `application/`：编排、任务应用服务、工具服务、记忆服务、轨迹服务、后台服务。
- `domain/`：枚举、工具契约、领域模型。
- `dto/`：Agent DTO 与响应契约。
- `entity/`：任务、步骤、事件、工具调用、记忆、评估记录实体。
- `infrastructure/tool/`：具体工具实现与注册表。
- `infrastructure/persistence/`：Schema Initializer 等持久化基础设施。
- `mapper/`：MyBatis Mapper 接口。
- XML Mapper 位于 `backend/src/main/resources/mapper/`

## 关键后端链路

- 任务 API：`AgentTaskController.java`
- 记忆 API：`AgentMemoryController.java`
- 工具 API：`AgentToolController.java`
- 后台 Agent API：`controller/admin/AdminAgentController.java`
- 任务应用服务：`AgentTaskApplicationService.java`
- 编排器：`AgentOrchestratorService.java`
- 工具服务：`AgentToolService.java`
- 轨迹/事件服务：`AgentTraceService.java`
- 后台服务：`AgentAdminService.java`
- 发布服务：`backend/src/main/java/com/hejulian/blog/service/AdminBlogService.java`
- 运行时配置：`AuthService.requireRagRuntimeOptions(...)`、`RagRuntimeContextHolder`、`DashScopeModelGateway`

## 标准任务生命周期

1. 前端通过 `POST /api/agent/tasks` 创建任务。
2. 后端写入 `agent_task`，初始状态为 pending。
3. 编排器异步执行。
4. 前端订阅 `/api/agent/tasks/{taskId}/stream`。
5. SSE 持续推送 progress / snapshot / done。
6. 编排器依次运行 planner、researcher、writer、reviewer、publisher。
7. publisher 保存任务笔记并发布文章。
8. 后端持久化历史 / 轨迹，并做相关缓存失效。
9. 前端在 Execution Trace 中渲染步骤、工具调用和最终文章。

## Agent 角色与工具

典型角色：

- Planner：生成计划，读取记忆摘要。
- Researcher：按搜索范围调用站内、知识库或联网工具。
- Writer：撰写文章草稿。
- Reviewer：清理、校验并润色草稿。
- Publisher：保存任务笔记并发布文章。

当前工具实现至少包括：

- `GetUserMemorySummaryTool`
- `SearchSitePostsTool`
- `SearchKnowledgeChunksTool`
- `WebSearchTool`
- `SaveTaskNoteTool`
- `GetPostDetailTool`
- `ListCategoriesTagsTool`

## 运行时与搜索规则

- 每次模型生成前都要确保用户 Qwen 运行时可用。
- `WEB_ONLY`：跳过本地站内 / chunk 工具，直接走 `web_search`。
- `LOCAL_AND_WEB`：本地工具和联网搜索一起用。
- `LOCAL_ONLY`：只用本地工具。
- 如果用户要求联网，但所选 Qwen 模型不支持 web search，要明确失败原因。

## 持久层与缓存检查

改动 Agent 数据时，至少检查：

- Agent 实体与 Mapper
- `AgentTaskMapper.xml` 及 step / event / tool / memory / eval 对应 XML
- `sql/blog_mysql_init.sql`
- `AgentSchemaInitializer.java`
- Agent 写入对 RAG 历史 / 会话缓存的影响
- Agent 发布文章后对首页 / 列表 / 详情缓存的驱逐

## SSE 与 nginx

Agent 流式接口需要特殊 nginx 配置：

- `/api/agent/tasks/{taskId}/stream`
- `proxy_buffering off`
- `proxy_cache off`
- 较长的 `proxy_read_timeout` / `proxy_send_timeout`
- `X-Accel-Buffering no`

如果是 Docker 前端镜像，要改 `frontend/nginx/default.conf.template`；只改 `frontend/nginx/default.conf` 不够。

## 常见坑

- 在 create / retry 请求里同步执行整条 Agent 流程，导致前端看不到中途进度。
- 任务已取消，但最终状态仍被写成 completed。
- 把计划、工具调用或调试文字原样发布到最终文章里。
- 保存了 `Draft for:` 之类的占位文本，而不是最终正文。
- 前端没有动态展示工具调用和多阶段轨迹。
- 发布文章后忘了驱逐首页相关缓存。
- 让普通用户访问 `/api/agent/**`。

## 验证

- 前端改动：`npm run build`
- 后端改动：本地 Maven，或在无 Maven 时执行 `docker compose build backend`
- 运行时问题：查看 `logs/YYYY-MM-DD/` 下的当日日志
