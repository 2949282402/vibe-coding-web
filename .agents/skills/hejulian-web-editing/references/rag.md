# RAG、Ask 与 `/knowledge` 参考

用于处理 `/knowledge` 页面、RAG 检索、纯 LLM Ask、来源/引用、聊天历史、重放、反馈、搜索范围和 SSE。

## 用户可见模式

`frontend/src/views/KnowledgeView.vue` 当前暴露三种响应模式：

- `RAG`：检索增强回答，带来源、引用、搜索范围、历史与反馈。
- `Ask`：纯大模型问答，发送 `answerMode: ASK`；后端跳过本地/联网检索，返回 `mode: ask`，不应带来源或引用。
- `Agent`：仅管理员可见，走 `/api/agent/**`；细节见 `references/agent.md`。

## 搜索范围

搜索范围独立于响应模式，只作用于 `RAG` 与 `Agent`，不作用于 `Ask`：

- `LOCAL_ONLY`：只做站内 / 知识库本地检索。
- `WEB_ONLY`：只走 Qwen 联网搜索。
- `LOCAL_AND_WEB`：本地检索与 Qwen 联网搜索同时参与。

## 前端链路

- 页面：`frontend/src/views/KnowledgeView.vue`
- API 封装：`frontend/src/api/blog.js`
- Markdown / 引用渲染：`frontend/src/utils/markdown.js`

关键前端行为：

- SSE 解析用的是 `fetch` + `ReadableStream`，不是 `EventSource`，这样才能带鉴权头。
- RAG / Ask 流式接口：`/api/public/rag/ask/stream`
- Agent 流式接口：`/api/agent/tasks/{taskId}/stream`
- 来源侧栏只对 RAG 结果有意义；Ask 结果不应显示来源。
- 会话列表与历史记录的本地刷新要与后端写入节奏保持一致。

## 后端链路

- 控制器：`backend/src/main/java/com/hejulian/blog/controller/RagController.java`
- DTO：`backend/src/main/java/com/hejulian/blog/dto/RagDtos.java`
- 应用服务：`backend/src/main/java/com/hejulian/blog/rag/application/RagApplicationService.java`
- 索引服务：`RagIndexingApplicationService.java`
- 运行时上下文：`RagRuntimeContextHolder.java`
- Qwen 网关：`DashScopeModelGateway.java`
- Prompt / 引用辅助：`RagPromptService.java`、`CitationGuardService.java`
- 检索：`KnowledgeRetrievalService.java`、`MybatisRagKnowledgeBaseRepository.java`、`QdrantVectorStore.java`
- 持久化：`RagChatSessionMapper`、`RagChatMessageMapper`、`RagChunkMapper` 及其 XML

## 需要保持的 DTO 契约

- `RagDtos.AskRequest`：`question`、`topK`、`sessionId`、`searchMode`、`answerMode`
- `answerMode: ASK` 表示纯模型对话
- `RagDtos.AskResponse`：包含 `sessionId`、`question`、`answer`、`mode`、`sources`、`history`、`strictCitation`、`searchMode`
- 聊天消息 `mode` 可能包含 `retrieval`、`llm`、`ask`、`agent`

## SSE 契约

后端发送 `StreamEvent`，包含：

- `type: meta`：初始状态与空回答
- `type: delta`：流式文本增量
- `type: done`：最终 `AskResponse`
- `type: error`：错误消息

如果流式响应看起来被“憋住”了，同时检查三层：

- 后端 `SseEmitter` 是否及时 flush 事件
- 前端解析器是否正确处理 event framing，并更新 `pendingTurn` / `result`
- nginx 是否对流式接口设置了 `proxy_buffering off` 与 `X-Accel-Buffering no`

## 历史、重放、反馈

相关接口包括：

- `POST /api/public/rag/ask`
- `POST /api/public/rag/ask/stream`
- `POST /api/public/rag/replay`
- `GET /api/public/rag/history`
- `GET /api/public/rag/sessions`
- 会话 rename / delete / restore / purge 系列接口
- `POST /api/public/rag/feedback`

调整这些流程时，要同步检查 RAG 历史与会话列表缓存失效。

## 常见坑

- 把 Ask 当成“没有来源的 RAG”。Ask 必须显式绕过检索。
- 对 Ask 结果强制做引用校验。Ask 应返回 `strictCitation=false`。
- 在 Ask 模式下把聊天模式开关也隐藏掉；正确做法是只隐藏搜索范围切换。
- 通过非 Qwen 或旧路径新增联网搜索逻辑。
- 改了响应结构，却没同步更新 `KnowledgeView.vue` 与 `frontend/src/api/blog.js`。

## 验证

- 前端：`npm run build`
- 后端：Maven 打包，或 `docker compose build backend`
- 涉及 SSE / nginx 缓冲配置时，要重建或重启前端容器，因为 nginx 模板烘焙在镜像里
