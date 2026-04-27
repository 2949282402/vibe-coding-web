# HeJulian Web Agent 技术方案

## 1. 文档目标

本文档面向 `hejulian-web` 项目，给出一套可逐步落地的 Agent 技术方案，重点覆盖以下当前热门能力：

- 多 Agent 协同
- 工具调用（Tool Calling / Function Calling）
- 记忆管理（Short-term / Long-term / Episodic Memory）
- Agent 可观测性（Trace / Replay / Metrics）
- 人机协同（Human-in-the-loop）
- 面向博客创作与知识问答的业务落地

本文档目标不是一次性重写现有 RAG，而是在现有博客系统、RAG 问答、登录鉴权、后台管理、Redis 缓存、MySQL 持久化的基础上，扩展出一套“可演示、可调试、可迭代”的 Agent 平台能力。

---

## 2. 当前项目基础与可复用能力

结合当前仓库结构，项目已经具备非常适合 Agent 技术落地的基础设施：

### 2.1 已有能力

- 前端已有 `/knowledge` 页面，可承载 Agent 对话、任务模式、执行过程展示
- 后端已有 `RagController` 和 `RagApplicationService`，具备现成的问答主链路
- 已有 SSE 流式问答能力，可用于实时展示 Agent 执行过程
- 已有登录用户体系，可按用户隔离记忆、任务、配置、工具权限
- 已有 MySQL 持久化，可承载任务、记忆、轨迹、反馈等结构化数据
- 已有 Redis 缓存，可承载短期记忆、执行状态、热数据缓存
- 已有 RAG Session / History / Replay / Feedback 相关机制，适合继续扩展为 Agent 工作流
- 已有管理后台，可承载 Agent Ops、反馈闭环、评估面板
- 已有文章、标签、分类、评论、上传等博客能力，天然适合作为 Agent 工具集

### 2.2 当前不足

- 现有 `/knowledge` 更偏单 Agent 问答，没有显式的角色协同机制
- 工具调用能力未抽象为统一 Tool Registry
- 记忆主要停留在会话历史，缺少长期偏好与情景记忆
- 缺少任务级可观测性与回放能力
- 缺少 Agent 安全边界与工具权限分级
- 缺少围绕“博客创作流程”的业务闭环

因此，建议在现有 RAG 基础上，引入 Agent Orchestration Layer，而不是直接替换原有问答链路。

---

## 3. 总体目标

本次 Agent 能力建设建议围绕一个清晰产品定位推进：

**把 `hejulian-web` 升级为“个人知识与博客创作 Agent 平台”。**

### 3.1 核心产品目标

- 用户可以在 `/knowledge` 中不只是问答，还可以提交一个可持续推进的任务
- 系统可以拆分多个 Agent 角色协同完成任务
- Agent 可以按需调用站内工具、知识检索工具、联网搜索工具、内容写入工具
- 系统能够对用户偏好和长期任务进行记忆
- 管理员能够在后台看到 Agent 执行轨迹、工具调用、失败原因、反馈闭环
- 最终形成“问答 -> 任务 -> 草稿 -> 审核 -> 发布”的内容生产链路

### 3.2 技术目标

- 保持现有 RAG 问答链路可用，新增 Agent 模式而非破坏式替换
- 支持单 Agent 和多 Agent 两种执行模式
- 构建统一工具抽象层，避免工具逻辑散落在业务服务中
- 构建三层记忆模型，支持热存储与持久化分层
- 引入 Trace / Event Log / Replay，提升可调试性
- 所有关键模块具备可灰度、可回滚、可降级能力

---

## 4. 推荐落地方向

推荐优先落地以下主方案：

## 4.1 主方向：多 Agent 博客创作助手

这是最适合当前项目定位的方案。用户输入一个目标，例如：

- “帮我写一篇 Spring Security 登录鉴权实践文章”
- “结合我站内文章，总结 Redis 在这个项目中的使用方式”
- “生成一篇适合发布到博客后台的初稿”

系统自动进入任务模式，由多个 Agent 协同完成：

- `Planner Agent`：拆解目标，生成执行计划
- `Research Agent`：检索站内知识、站内文章、必要时执行网络搜索
- `Writer Agent`：生成博客提纲与正文
- `Reviewer Agent`：做事实校验、引用补足、语言润色
- `Publisher Agent`：生成标签、摘要、SEO 描述，并可写入后台草稿区

## 4.2 辅方向：分层记忆个人知识助理

围绕登录用户构建记忆层，让系统逐步记住：

- 用户偏好什么风格的回答
- 用户是否偏好中文 / 英文
- 用户更关注哪些技术栈
- 用户是否正在推进某个长期主题
- 用户过去做过哪些创作任务

## 4.3 支撑方向：Agent 可观测与回放平台

提供后台 `Agent Ops` 页面，让管理员看到：

- 每个任务的执行轨迹
- 每个 Agent 的输入输出摘要
- 每次工具调用详情
- 每段记忆命中情况
- 失败与重试路径
- 执行耗时、成功率、缓存命中率

---

## 5. 总体技术架构

建议在当前 RAG 之上新增一个 Agent 中间层，结构如下：

```text
Frontend (Vue)
  ├─ KnowledgeView: 聊天模式 / 任务模式 / 执行轨迹展示
  ├─ Admin Agent Ops: 任务轨迹 / 工具调用 / 记忆管理 / 回放
  └─ Admin Feedback/Eval: 反馈闭环 / 评估看板

Backend (Spring Boot)
  ├─ Controller Layer
  │   ├─ RagController (保留现有问答)
  │   └─ AgentTaskController (新增 Agent 任务接口)
  │
  ├─ Application Layer
  │   ├─ RagApplicationService (现有能力)
  │   ├─ AgentOrchestratorService
  │   ├─ AgentTaskApplicationService
  │   ├─ AgentMemoryApplicationService
  │   └─ AgentTraceApplicationService
  │
  ├─ Domain Layer
  │   ├─ orchestration/
  │   ├─ tools/
  │   ├─ memory/
  │   ├─ trace/
  │   └─ eval/
  │
  ├─ Infrastructure Layer
  │   ├─ MyBatis Mapper/XML
  │   ├─ Redis Cache
  │   ├─ DashScope / LLM Gateway
  │   ├─ Qdrant / Vector Store
  │   └─ Optional external tool adapters
  │
  └─ Persistence
      ├─ MySQL: 任务、记忆、轨迹、评估、工具日志
      └─ Redis: 短期记忆、热状态、执行中缓存
```

### 5.1 核心设计原则

- **保守演进**：先保留现有 `/api/public/rag/*` 能力，再新增 `/api/agent/*`
- **能力分层**：把编排、工具、记忆、追踪拆开，避免全部堆进 `RagApplicationService`
- **用户隔离**：任务、记忆、工具权限、配置必须按用户隔离
- **可回放**：重要 Agent 决策和工具调用都要记录摘要事件
- **可降级**：多 Agent 失败时可回退为单 Agent 或标准 RAG
- **先结构化再智能化**：先把任务、工具、记忆、事件模型搭起来，再优化 prompt

---

## 6. 功能设计

## 6.1 前台：Knowledge 页升级为双模式

### 模式 A：聊天模式

延续现有问答方式，适合快速提问。

### 模式 B：任务模式

新增一个任务入口，用户提交以下信息：

- 任务标题
- 任务目标
- 输出类型（问答 / 提纲 / 博客初稿 / 审校稿 / 后台草稿）
- 检索范围（仅站内 / 站内 + 网络）
- 是否启用多 Agent
- 是否允许写入草稿箱

### 任务模式展示内容

- 当前任务状态：待执行 / 执行中 / 已完成 / 失败 / 已取消
- 当前 Agent：planner / researcher / writer / reviewer / publisher
- 每步进度状态
- 已调用的工具列表
- 命中的记忆摘要
- 最终结果与引用来源
- 一键保存为草稿 / 重新规划 / 人工修订

## 6.2 后台：Agent Ops 页面

新增后台页面用于观察和管理 Agent 执行：

- 任务列表
- 任务详情页
- 事件时间线
- 工具调用日志
- 记忆命中记录
- 失败分析面板
- 任务回放
- 按用户 / 时间 / 状态 / 工具类型筛选

## 6.3 后台：Memory 管理页面

管理员或用户可查看：

- 长期偏好记忆
- 情景任务记忆
- 自动抽取的用户事实
- 低置信度记忆
- 可手动删除或冻结的记忆项

## 6.4 后台：Eval / Feedback 页面增强

在现有反馈页面基础上增强：

- 反馈关联到具体任务 / 具体 Agent / 具体工具调用
- 统计高频失败问题
- 标记“事实错误”“引用不足”“工具选择错误”“风格不符合预期”等原因
- 支持导出评估数据
- 支持形成优化建议单

---

## 7. 多 Agent 协同设计

## 7.1 Agent 角色定义

建议第一期只启用 4 到 5 个固定角色，不要一开始做过度动态化：

### 1) Planner Agent

职责：

- 理解用户目标
- 判断任务类型
- 决定是否需要多 Agent
- 生成步骤计划
- 为后续角色分派子任务

输入：

- 用户问题 / 任务目标
- 当前会话上下文
- 用户偏好记忆
- 可用工具清单

输出：

- 任务计划
- 角色分工
- 期望输出结构
- 是否允许联网 / 写草稿

### 2) Research Agent

职责：

- 调用站内搜索、文章检索、RAG 检索、联网搜索工具
- 汇总证据
- 输出结构化 research notes

输出内容建议结构：

- 核心结论
- 证据点列表
- 来源列表
- 不确定点
- 建议补充搜索项

### 3) Writer Agent

职责：

- 根据任务计划与研究结果生成内容
- 对博客文章输出提纲、正文、摘要草案

### 4) Reviewer Agent

职责：

- 检查事实一致性
- 检查引用完整性
- 检查语言表达质量
- 判断是否需要回退给 Research Agent 继续补充证据

### 5) Publisher Agent（可选）

职责：

- 生成分类、标签、摘要、SEO 描述
- 将内容写入后台草稿区
- 返回草稿 ID

## 7.2 编排方式

建议采用**有中心编排器的顺序协同模式**，而不是多 Agent 自由对话。

推荐链路：

```text
User Goal
  -> Planner
  -> Research
  -> Writer
  -> Reviewer
  -> Publisher(optional)
  -> Final Response
```

优点：

- 易于调试
- 易于记录轨迹
- 易于限制 token 成本
- 易于失败恢复
- 更适合现有 Spring Boot 结构

## 7.3 失败恢复机制

当某一步失败时：

- 工具失败：重试 1~2 次，仍失败则记录事件并降级
- Research 证据不足：回退到 Planner 进行补检索
- Writer 输出质量差：交给 Reviewer 标注原因并触发重写
- 多 Agent 编排失败：降级为单 Agent + RAG 模式
- 写草稿失败：保留文本结果，提示用户手动保存

---

## 8. 工具调用体系设计

## 8.1 目标

把现有站内能力抽象成 Agent 可调用工具，而不是让 Agent 直接耦合具体 Service。

## 8.2 Tool Registry 设计

新增工具注册中心，包含：

- 工具唯一标识
- 工具描述
- 参数 Schema
- 权限级别
- 返回结构
- 是否允许普通用户使用
- 是否允许写操作
- 超时策略
- 降级策略

建议抽象接口：

```text
AgentTool
  - name()
  - description()
  - inputSchema()
  - permissionLevel()
  - execute(ToolContext, JsonNode args)
```

## 8.3 第一阶段建议工具清单

### 只读工具

- `search_site_posts`
  - 按关键词搜索站内文章
- `get_post_detail`
  - 获取指定文章详情
- `list_categories_tags`
  - 获取分类与标签信息
- `search_knowledge_chunks`
  - 调现有 RAG 检索能力返回 chunk
- `get_user_recent_tasks`
  - 获取用户最近任务
- `get_user_memory_summary`
  - 获取用户长期偏好摘要
- `web_search`
  - 基于现有 Qwen 搜索能力做联网搜索

### 写入工具

- `save_post_draft`
  - 写入后台文章草稿
- `save_task_note`
  - 保存任务中间结论
- `pin_memory_item`
  - 将某条信息升级为长期记忆

## 8.4 工具权限控制

工具权限建议分三层：

- `READ_ONLY`：普通登录用户可调用
- `USER_WRITE`：需要用户显式授权，允许写自己的数据
- `ADMIN_WRITE`：仅管理员可用，例如直接创建后台内容

前端需要明确展示：

- 当前任务启用了哪些工具
- 是否含写入工具
- 写入前是否需要确认

---

## 9. 记忆管理设计

## 9.1 记忆目标

让系统能记住“对当前用户长期有价值”的信息，同时避免上下文无限增长。

## 9.2 三层记忆模型

### A. 短期记忆（Short-term Memory）

内容：

- 当前会话最近若干轮对话
- 当前任务执行状态
- 当前任务的中间产物

存储：

- Redis 优先
- 部分重要摘要写 MySQL

用途：

- 保障多轮连续性
- 支持 Agent 任务中断恢复

### B. 长期记忆（Long-term Memory）

内容：

- 用户语言偏好
- 用户输出风格偏好
- 用户常关注技术栈
- 用户常用任务类型
- 用户确认过的重要事实

存储：

- MySQL 主存储
- 可选向量化字段做语义召回

用途：

- 个性化回答
- 个性化创作风格
- 减少重复配置

### C. 情景记忆（Episodic Memory）

内容：

- 某个长期任务的背景
- 某篇正在创作的文章历史
- 某次失败尝试的上下文
- 某次研究任务的中间结论

存储：

- MySQL + Redis 热缓存

用途：

- 支持长期任务持续推进
- 支持“接着上次继续做”

## 9.3 记忆写入策略

不是每轮对话都写长期记忆，建议通过 `Memory Extractor` 有选择地提取：

判断维度：

- 是否为稳定偏好
- 是否被用户明确确认
- 是否会在未来重复使用
- 是否涉及隐私或不适合长期保存
- 置信度是否足够

## 9.4 记忆召回策略

每次任务开始时：

1. 先取当前会话短期记忆
2. 再取当前主题相关的情景记忆
3. 再取少量高置信度长期记忆
4. 对记忆做压缩摘要后送给 Planner

### 记忆注入原则

- 不直接注入所有历史数据
- 只注入与当前任务强相关的摘要
- 给每条记忆打来源标签和置信度
- 支持在前端显示“本次回答命中了哪些记忆”

---

## 10. 可观测性与回放设计

## 10.1 目标

让 Agent 系统不只是“能跑”，而且“可调试、可解释、可追责”。

## 10.2 记录内容

建议按任务记录以下事件：

- 任务创建
- 任务开始执行
- Planner 输出计划
- 每个 Agent 开始 / 结束
- 每次工具调用请求与结果摘要
- 记忆召回结果
- LLM 输出摘要
- 重试事件
- 降级事件
- 用户中断事件
- 最终完成 / 失败

## 10.3 事件模型

每个事件记录：

- `taskId`
- `sessionId`
- `eventType`
- `agentRole`
- `stepIndex`
- `payloadSummary`
- `status`
- `latencyMs`
- `createdAt`

## 10.4 回放能力

后台任务详情页可按时间线回放：

- 展示步骤顺序
- 展示工具输入输出摘要
- 展示记忆命中摘要
- 展示最终结果与中间版本差异

注意：

- 默认只展示摘要，避免泄漏完整 prompt 或敏感信息
- 对管理员可提供更细粒度日志查看开关

---

## 11. 数据库设计建议

以下为建议新增表，第一期不要求一次性全部上线，可按阶段落地。

## 11.1 Agent 任务表 `agent_task`

用途：记录用户发起的 Agent 任务。

建议字段：

- `id`
- `user_id`
- `session_id`
- `task_type`：CHAT / OUTLINE / BLOG_DRAFT / REVIEW / RESEARCH
- `title`
- `goal`
- `status`：PENDING / RUNNING / COMPLETED / FAILED / CANCELED
- `execution_mode`：SINGLE_AGENT / MULTI_AGENT
- `search_scope`：LOCAL_ONLY / LOCAL_AND_WEB
- `allow_draft_write`
- `current_step`
- `final_output_summary`
- `error_message`
- `started_at`
- `completed_at`
- `created_at`
- `updated_at`

## 11.2 Agent 步骤表 `agent_task_step`

用途：记录每个 Agent 步骤。

建议字段：

- `id`
- `task_id`
- `step_index`
- `agent_role`
- `step_name`
- `status`
- `input_summary`
- `output_summary`
- `retry_count`
- `latency_ms`
- `started_at`
- `completed_at`
- `created_at`

## 11.3 Agent 事件表 `agent_task_event`

用途：记录所有轨迹事件。

建议字段：

- `id`
- `task_id`
- `step_id`
- `event_type`
- `agent_role`
- `payload_json`
- `payload_summary`
- `status`
- `latency_ms`
- `created_at`

## 11.4 工具调用表 `agent_tool_call`

用途：记录工具执行情况。

建议字段：

- `id`
- `task_id`
- `step_id`
- `tool_name`
- `permission_level`
- `request_json`
- `response_summary`
- `success`
- `error_message`
- `latency_ms`
- `created_at`

## 11.5 记忆表 `agent_memory`

用途：记录长期记忆与情景记忆。

建议字段：

- `id`
- `user_id`
- `memory_scope`：LONG_TERM / EPISODIC
- `topic_key`
- `memory_type`：PREFERENCE / FACT / TASK_CONTEXT / STYLE / INTEREST
- `content`
- `content_summary`
- `confidence_score`
- `source_type`：USER_EXPLICIT / INFERRED / TASK_EXTRACTED / ADMIN_CONFIRMED
- `source_ref_id`
- `is_pinned`
- `is_deleted`
- `last_hit_at`
- `created_at`
- `updated_at`

## 11.6 记忆命中表 `agent_memory_hit`

用途：记录一次任务命中了哪些记忆。

建议字段：

- `id`
- `task_id`
- `memory_id`
- `hit_reason`
- `used_in_step`
- `created_at`

## 11.7 Agent 评估表 `agent_eval_record`

用途：记录任务质量评估和人工反馈。

建议字段：

- `id`
- `task_id`
- `user_id`
- `score_overall`
- `score_grounding`
- `score_helpfulness`
- `score_style`
- `issue_types`
- `feedback_note`
- `created_at`

---

## 12. 后端实现方案

## 12.1 推荐包结构

建议在 `backend/src/main/java/com/hejulian/blog/rag/` 同级或其下新增以下模块：

```text
backend/src/main/java/com/hejulian/blog/agent/
  ├─ controller/
  ├─ application/
  ├─ domain/
  │   ├─ orchestration/
  │   ├─ tool/
  │   ├─ memory/
  │   ├─ trace/
  │   └─ eval/
  ├─ infrastructure/
  │   ├─ persistence/
  │   ├─ llm/
  │   ├─ cache/
  │   └─ tool/
  ├─ dto/
  └─ mapper/
```

如果希望与现有 RAG 强绑定，也可以放在 `rag/agent/` 下，但更推荐独立为 `agent/`，避免未来边界模糊。

## 12.2 关键服务划分

### `AgentTaskApplicationService`

职责：

- 创建任务
- 查询任务详情
- 获取任务列表
- 控制取消 / 重试
- 汇总前端展示 DTO

### `AgentOrchestratorService`

职责：

- 执行多 Agent 编排
- 调用 Planner / Research / Writer / Reviewer
- 处理失败恢复与降级

### `AgentToolService`

职责：

- 管理工具注册
- 做参数校验
- 执行权限校验
- 记录调用日志

### `AgentMemoryService`

职责：

- 记忆提取
- 记忆召回
- 记忆压缩
- 记忆写入与更新

### `AgentTraceService`

职责：

- 记录任务事件
- 查询任务轨迹
- 生成回放视图

## 12.3 与现有 RAG 的集成点

### 与 `RagApplicationService` 集成

建议把现有 RAG 能力作为一个可复用工具或下游能力：

- `search_knowledge_chunks`
- `ask_rag_once`
- `stream_rag_answer`

这样 Agent 层不直接复制 RAG 逻辑，而是复用现有检索 / 生成能力。

### 与 `AuthService` 集成

用于：

- 获取当前用户配置
- 获取模型配置
- 获取用户权限

### 与 `AdminBlogService` 集成

用于：

- 保存文章草稿
- 生成标签 / 分类候选后落库

---

## 13. 前端实现方案

## 13.1 `/knowledge` 页面改造

建议在 `frontend/src/views/KnowledgeView.vue` 上分阶段增强，而不是拆掉重写。

### 第一阶段新增 UI 区块

- 模式切换：聊天模式 / 任务模式
- 任务表单：目标、输出类型、搜索范围、是否启用多 Agent
- 执行状态区：当前阶段、进度条、Agent 状态卡片
- 工具调用区：展示已经调用的工具
- 记忆命中区：展示命中的用户偏好 / 任务记忆
- 最终结果区：正文结果、引用来源、草稿保存按钮

### 第二阶段新增交互

- 实时展示步骤状态（可通过 SSE 或轮询）
- 支持中断任务
- 支持一键继续上次任务
- 支持任务回放
- 支持人工介入修改计划后重跑

## 13.2 后台页面建议

### 新增页面 1：`frontend/src/views/admin/AgentOpsView.vue`

展示：

- 任务列表
- 状态筛选
- 任务详情抽屉 / 页面
- 工具日志
- 失败事件
- 回放时间线

### 新增页面 2：`frontend/src/views/admin/AgentMemoryManageView.vue`

展示：

- 用户记忆列表
- 记忆类型筛选
- 置信度筛选
- 删除 / Pin / 冻结

### 新增页面 3：`frontend/src/views/admin/AgentEvalView.vue`

展示：

- 反馈统计
- 问题分类统计
- 用户满意度趋势
- 各 Agent 成功率

---

## 14. API 设计建议

## 14.1 用户侧接口

### 创建任务

`POST /api/agent/tasks`

请求示例：

```json
{
  "title": "Spring Security 博客初稿",
  "goal": "基于站内知识和现有项目经验，生成一篇适合发布的 Spring Security 登录鉴权实践博客初稿",
  "taskType": "BLOG_DRAFT",
  "executionMode": "MULTI_AGENT",
  "searchScope": "LOCAL_AND_WEB",
  "allowDraftWrite": true
}
```

### 查询任务详情

`GET /api/agent/tasks/{taskId}`

### 查询任务列表

`GET /api/agent/tasks?page=1&pageSize=20&status=RUNNING`

### 取消任务

`POST /api/agent/tasks/{taskId}/cancel`

### 重试任务

`POST /api/agent/tasks/{taskId}/retry`

### 获取任务流式事件

`GET /api/agent/tasks/{taskId}/stream`

### 保存结果为草稿

`POST /api/agent/tasks/{taskId}/save-draft`

## 14.2 用户记忆接口

### 获取我的记忆摘要

`GET /api/agent/memories/me`

### 删除某条记忆

`DELETE /api/agent/memories/{id}`

### Pin 某条记忆

`POST /api/agent/memories/{id}/pin`

## 14.3 管理员接口

### 查询任务轨迹

`GET /api/admin/agent/tasks/{taskId}/trace`

### 查询工具调用日志

`GET /api/admin/agent/tool-calls`

### 查询记忆命中统计

`GET /api/admin/agent/memory-hits`

### 查询评估统计

`GET /api/admin/agent/evals/summary`

---

## 15. 典型执行流程

## 15.1 场景一：生成博客初稿

用户输入：

“帮我写一篇 Redis 在本项目中的使用实践文章，适合发到博客后台。”

执行流程：

1. 前端提交任务到 `POST /api/agent/tasks`
2. 后端创建 `agent_task` 记录，状态为 `PENDING`
3. `AgentOrchestratorService` 启动任务
4. `Planner Agent` 输出计划：
   - 检索项目中 Redis 相关实现
   - 识别站内已有相关内容
   - 生成文章提纲
   - 生成初稿
   - 复核与补充引用
5. `AgentMemoryService` 召回用户偏好，例如“偏好中文、偏技术细节、喜欢结构化标题”
6. `Research Agent` 调用工具：
   - `search_site_posts`
   - `search_knowledge_chunks`
   - 必要时 `web_search`
7. `Writer Agent` 基于研究结果生成初稿
8. `Reviewer Agent` 检查：
   - 引用是否充分
   - 内容是否与站内事实冲突
   - 是否存在未经验证的结论
9. 如果用户开启允许写入：`Publisher Agent` 调用 `save_post_draft`
10. 前端实时展示任务状态与工具调用过程
11. 最终返回：
   - 博客初稿
   - 引用来源
   - 生成标签建议
   - 草稿 ID

## 15.2 场景二：长期任务续写

用户输入：

“继续昨天那篇 JWT 文章。”

执行流程：

1. 系统先召回情景记忆，找到最近相关任务
2. 恢复上次的任务背景、提纲、已完成段落
3. Planner 只规划剩余步骤
4. Writer 基于历史内容继续生成
5. Reviewer 做连续性校验

这正是情景记忆最有价值的场景。

---

## 16. 实施阶段建议

建议按 4 个阶段推进，避免一下子做太大。

## Phase 1：最小可运行 Agent MVP

目标：先把任务、编排、工具和展示链路打通。

### 范围

- 新增 `agent_task`、`agent_task_step`、`agent_task_event`
- 新增 `/api/agent/tasks` 基础接口
- 实现 `Planner + Research + Writer + Reviewer` 固定流程
- 实现 3~4 个只读工具
- `/knowledge` 增加任务模式和执行状态区
- 后台增加简单任务列表页

### 成功标准

- 用户可以发起一个博客初稿任务
- 前端能看到步骤状态
- 后端能记录任务轨迹
- 最终能生成结构化结果

## Phase 2：记忆系统上线

目标：让 Agent 有“记住用户”的能力。

### 范围

- 新增 `agent_memory`、`agent_memory_hit`
- 实现偏好记忆抽取与召回
- 支持“继续上次任务”
- 前端展示记忆命中摘要
- 管理后台增加记忆管理页

### 成功标准

- 同一用户第二次执行任务时，输出风格能体现个性化
- 用户可以看到系统记住了什么
- 管理员可以删除或修正记忆

## Phase 3：工具体系和写入闭环

目标：把 Agent 从“会分析”升级成“会办事”。

### 范围

- 引入 Tool Registry
- 增加写入型工具：`save_post_draft`
- 支持人工确认后写草稿
- 打通后台文章草稿区
- 引入权限模型

### 成功标准

- Agent 结果可一键落到草稿箱
- 写入行为可审计、可回放
- 非管理员无法越权写后台内容

## Phase 4：可观测与评估闭环

目标：把系统做成可持续优化的平台。

### 范围

- 完整 Agent Ops 页面
- 工具调用分析
- 失败类型聚类
- 评估与反馈统计
- 回放与问题定位能力

### 成功标准

- 能快速定位任务失败点
- 能看到哪些工具最常失败
- 能形成明确优化方向

---

## 17. 关键实现流程

## 17.1 后端实现流程

### 第一步：建模与持久化

- 新增实体 / DTO / Mapper / XML
- 在 `sql/blog_mysql_init.sql` 追加 Agent 相关建表语句
- 若需要兼容老库，可新增 `AgentSchemaInitializer`

### 第二步：任务主链路

- 新增 `AgentTaskController`
- 新增 `AgentTaskApplicationService`
- 实现任务创建、详情、列表、取消接口

### 第三步：编排器实现

- 新增 `AgentOrchestratorService`
- 先写死 `planner -> research -> writer -> reviewer` 顺序链
- 每一步都记录 event

### 第四步：工具注册与接入

- 设计工具接口
- 先接入站内文章搜索、RAG 检索、联网搜索
- 每次调用都写 `agent_tool_call`

### 第五步：前端任务模式联调

- 增加任务表单
- 增加任务详情状态卡
- 联调轮询或 SSE

### 第六步：记忆系统接入

- 实现记忆提取器
- 任务启动前召回记忆
- 任务完成后更新记忆

### 第七步：回放与后台运维页

- 查询任务 event 和 tool call
- 在后台做时间线展示

## 17.2 前端实现流程

### 第一步

在 `KnowledgeView.vue` 新增“聊天 / 任务”模式切换。

### 第二步

新增任务发起表单和任务状态展示卡。

### 第三步

接入任务详情轮询或 SSE。

### 第四步

补充工具调用日志、记忆命中卡片、最终结果卡片。

### 第五步

新增后台 Agent Ops 页面与路由。

---

## 18. Prompt 与执行策略建议

## 18.1 先固定模板，不要过早追求复杂 prompt 编排

第一期建议使用固定 Prompt 模板：

- Planner Prompt
- Research Prompt
- Writer Prompt
- Reviewer Prompt

每个 Prompt 包含：

- 角色说明
- 当前任务目标
- 可用输入
- 输出 JSON 结构要求
- 失败与不确定性表达要求

## 18.2 优先结构化输出

建议各 Agent 输出 JSON 或半结构化对象，例如：

- Planner 输出 `steps[]`
- Research 输出 `findings[]`, `sources[]`, `gaps[]`
- Reviewer 输出 `issues[]`, `revisionAdvice[]`

这样更适合后端编排，也更利于前端展示。

## 18.3 重要原则

- 不让 Agent 直接访问数据库层
- 不让 Agent 自由拼接内部 API
- 所有外部能力都通过工具层暴露
- 所有写操作都需经过权限校验和审计记录

---

## 19. 安全与风控建议

## 19.1 工具调用安全

- 写工具必须权限校验
- 高风险写操作需要用户确认
- 工具超时与异常要隔离，不拖垮主任务
- 对联网搜索结果做来源标注

## 19.2 记忆安全

- 敏感信息不自动转长期记忆
- 低置信度信息默认不长期保存
- 用户可查看和删除自己的记忆
- 管理员审查能力与普通用户视图要隔离

## 19.3 Prompt 注入与工具误用

- Research 输出只作为“证据候选”，不能天然视为可信
- Reviewer 需要负责事实校验和来源检查
- 工具参数严格走 Schema 校验，不能直接信任模型返回

---

## 20. 性能与成本建议

## 20.1 成本控制

- Planner / Reviewer 可以优先使用较低成本模型
- 仅在需要写作与润色时调用更强模型
- 对任务步骤做结果缓存
- 对站内检索结果做缓存
- 对记忆召回结果做压缩摘要，避免上下文膨胀

## 20.2 性能优化

- 任务创建后异步执行
- 前端使用 SSE 获取进度事件
- Redis 维护任务热状态和短期记忆
- 事件摘要和工具摘要异步落库

---

## 21. 测试建议

## 21.1 后端测试

建议新增以下测试：

- `AgentTaskApplicationService` 任务创建与状态流转测试
- `AgentOrchestratorService` 编排流程测试
- `AgentToolService` 参数校验与权限测试
- `AgentMemoryService` 记忆提取与召回测试
- Controller 接口测试

## 21.2 前端测试

建议覆盖：

- 任务模式表单提交
- 任务进度展示
- 工具调用列表渲染
- 记忆命中卡片展示
- 保存草稿交互

## 21.3 联调验证清单

- 多 Agent 流程是否可完成
- 工具调用是否有日志
- 失败是否能降级
- 记忆是否按用户隔离
- 草稿写入是否可审计
- 回放页面是否能复原关键步骤

---

## 22. 推荐首个 MVP 范围

如果只做一个最值得展示的 MVP，建议定义为：

**“多 Agent 博客创作助手”**

### MVP 功能清单

- `/knowledge` 新增任务模式
- 支持创建博客初稿任务
- 支持 `Planner -> Research -> Writer -> Reviewer` 固定流程
- 支持 4 个工具：
  - `search_site_posts`
  - `get_post_detail`
  - `search_knowledge_chunks`
  - `web_search`
- 支持基本任务轨迹记录
- 支持将结果保存到后台草稿区
- 支持最基础的用户偏好记忆

### 为什么先做这个

- 最贴合当前项目业务
- 演示效果强
- 能同时覆盖多 Agent、工具调用、记忆管理三个热点能力
- 复用现有博客后台和 RAG 最充分

---

## 23. 建议涉及的文件与模块

### 前端重点文件

- `frontend/src/views/KnowledgeView.vue`
- `frontend/src/api/blog.js`
- `frontend/src/router/index.js`
- `frontend/src/views/admin/` 下新增 Agent 相关页面

### 后端重点文件

- `backend/src/main/java/com/hejulian/blog/controller/RagController.java`
- `backend/src/main/java/com/hejulian/blog/rag/application/RagApplicationService.java`
- 新增 `backend/src/main/java/com/hejulian/blog/agent/` 模块
- `backend/src/main/resources/mapper/` 新增 Agent mapper XML
- `sql/blog_mysql_init.sql`

---

## 24. 最终结论

对于 `hejulian-web`，最合适的 Agent 演进路线不是做一个抽象的“万能助手”，而是做一个：

**基于站内知识、具备多 Agent 协同、支持工具调用、拥有分层记忆、可写入博客草稿、可被后台观察和回放的个人内容创作 Agent 平台。**

这条路线有三个优势：

- 与你现有技术栈和业务结构高度贴合
- 能完整覆盖当前热门 Agent 技术关键词
- 每个阶段都能形成可演示、可上线、可继续扩展的成果

---

## 25. 下一步推荐

建议按照下面顺序继续推进：

1. 先确认 MVP 范围，只做“多 Agent 博客初稿生成”
2. 明确第一期数据库表和接口定义
3. 先打通后端任务主链路和 Tool Registry
4. 再改造 `KnowledgeView.vue` 做任务模式
5. 最后补后台 Agent Ops 页面和记忆管理页

如果继续细化，下一份文档建议输出为：

- 数据库表结构 SQL 草案
- 后端类图与接口 DTO 草案
- 前端页面信息架构与交互稿
- MVP 开发任务拆解清单
