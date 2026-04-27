# 第一阶段工程工单清单

## 一、目标

把 P0 方案继续拆成可分配、可验收、可并行推进的工程工单。

## 工单 A1：前台 `/knowledge` 移除 Agent 一级入口

### 范围

- `frontend/src/views/KnowledgeView.vue`
- `frontend/src/api/blog.js`
- `frontend/src/router/index.js`

### 任务

- 移除普通用户可见的 `Agent` 模式切换 UI。
- 保留现有 RAG/Ask 调用能力，但前台默认主路径只突出站内知识问答。
- 确保非管理员不会通过页面路径进入 Agent 工作流。

### 验收

- 前台页面不再展示 Agent 主入口。
- 原有 RAG 会话能力保持可用。
- 不影响管理员后台入口。

## 工单 A2：Ask 降级为高级选项

### 范围

- `frontend/src/views/KnowledgeView.vue`

### 任务

- 将 `Ask` 从主模式切换区移入高级设置。
- 默认文案统一为“基于本站内容提问”。
- Ask 启用后，明确提示其为非站内知识回答。

### 验收

- 首屏不再平铺展示 Ask。
- Ask 仍然可切换、可提交、可流式返回。

## 工单 A3：问答首页态与引导区

### 范围

- `frontend/src/views/KnowledgeView.vue`
- 可复用样式文件：`frontend/src/styles/global.css`

### 任务

- 增加页面初始空态。
- 展示示例问题、能力说明、最近会话入口。
- 保证移动端与桌面端均可用。

### 验收

- 首次进入页面不再是纯空白聊天区域。
- 用户可以一键点击示例问题发起会话。

## 工单 B1：RAG 响应结构扩展

### 范围

- `backend/src/main/java/com/hejulian/blog/dto/RagDtos.java`
- `backend/src/main/java/com/hejulian/blog/controller/RagController.java`
- `backend/src/main/java/com/hejulian/blog/rag/application/RagApplicationService.java`

### 任务

- 在 `AskResponse` 中新增：
  - `sourceType`
  - `retrievalHitCount`
  - `usedWebSearch`
  - `latencyMs`
  - `confidenceLevel`
- 保持旧字段兼容，避免前端一次性切换风险。

### 验收

- 普通问答与 SSE `done` 结果都返回新增字段。
- 旧字段仍可正常解析。

## 工单 B2：可信度与来源规则计算

### 范围

- `backend/src/main/java/com/hejulian/blog/rag/application/RagApplicationService.java`
- `backend/src/main/java/com/hejulian/blog/rag/application/CitationGuardService.java`
- `backend/src/main/java/com/hejulian/blog/rag/application/KnowledgeRetrievalService.java`

### 任务

- 基于召回数量、来源组成、是否启用 Web Search、是否存在引用，生成 `confidenceLevel`。
- 明确区分：`local`、`web`、`mixed`、`none`。
- Ask 模式固定返回 `sourceType=none`。

### 验收

- 无本地知识命中时不会误标为站内回答。
- Web Only、弱召回、Ask 三类结果均可被正确区分。

## 工单 B3：前端问答元信息展示

### 范围

- `frontend/src/views/KnowledgeView.vue`
- `frontend/src/api/blog.js`

### 任务

- 渲染来源类型、可信度、耗时、命中来源数。
- 根据 `sourceType` 和 `confidenceLevel` 显示不同提示文案。
- 低可信度结果添加警示样式。

### 验收

- 每条完成态回答均有元信息展示区。
- 不影响 SSE 增量渲染。

## 工单 C1：Agent 任务终态调整为草稿流

### 范围

- `backend/src/main/java/com/hejulian/blog/agent/application/AgentTaskApplicationService.java`
- `backend/src/main/java/com/hejulian/blog/agent/application/AgentOrchestratorService.java`
- `backend/src/main/java/com/hejulian/blog/service/AdminBlogService.java`

### 任务

- 将 Agent 执行完成后的默认动作改为“保存草稿”。
- 阻断任务完成后直接发布文章的路径。
- 为任务新增 `DRAFT_READY` 等状态。

### 验收

- Agent 运行完成后，前台文章列表无新增已发布内容。
- 后台可见草稿就绪状态。

## 工单 C2：Agent 草稿审核接口

### 范围

- Agent controller / dto / mapper / service
- 必要的 SQL 初始化与 mapper XML

### 任务

- 新增草稿列表接口。
- 新增审核通过发布接口。
- 新增驳回接口。
- 记录审核人、审核时间、驳回原因。

### 验收

- 草稿可查询、可发布、可驳回。
- 非管理员无法调用接口。

## 工单 C3：后台草稿审核页

### 范围

- 新增 `frontend/src/views/admin/AgentDraftReviewView.vue`
- `frontend/src/api/admin.js`
- `frontend/src/router/index.js`

### 任务

- 展示草稿列表、状态、生成时间、审核动作。
- 支持预览、发布、驳回。
- 支持跳转执行轨迹。

### 验收

- 管理员能从后台完成 Agent 草稿审核闭环。

## 工单 D1：问答埋点表与基础统计

### 范围

- `sql/blog_mysql_init.sql`
- 后端 entity / mapper / XML / service

### 任务

- 新增 `rag_query_event` 表。
- 记录模式、来源、耗时、是否 helpful 等基础字段。
- 打通后台统计查询。

### 验收

- 能按时间范围统计问答量、帮助率、来源分布。

## 工单 D2：知识库运营后台 MVP

### 范围

- 新增 `frontend/src/views/admin/KnowledgeOpsView.vue`
- 后端统计接口

### 任务

- 展示问答量、帮助率、平均耗时。
- 展示无命中问题和低帮助率问题。
- 展示 Web Search 使用占比。

### 验收

- 管理员可以用后台数据判断知识问答是否有效。

## 二、并行建议

建议并行方式：

- 前端 1：`/knowledge` 页面收敛 + 元信息展示
- 前端 2：后台草稿审核页 + 运营页
- 后端 1：RAG 返回结构与规则增强
- 后端 2：Agent 草稿流与审核接口
- 数据库：埋点表与状态字段升级

## 三、测试清单

- `/knowledge` 页面主流程回归
- Ask / RAG / SSE 联调
- Agent 草稿生成、驳回、发布状态流转
- 后台管理员权限校验
- 发布后缓存失效与前台文章可见性