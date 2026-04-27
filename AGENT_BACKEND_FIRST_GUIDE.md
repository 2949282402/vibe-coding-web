# Agent 后端优先开发指南

> 目标：先完成后端，再开发前端。本文档按“先能跑、再完善、再可观测、最后联调前端”的顺序组织，方便直接照着做。

## 1. 开发总原则

### 1.1 后端先行的原因

- Agent 的核心能力都在后端：任务编排、工具调用、记忆管理、审计回放、权限控制
- 前端只负责展示和交互，依赖后端返回稳定的数据结构
- 先把后端做稳，前端只需要接接口和展示状态，不会返工

### 1.2 本阶段目标

先完成一个可用的 MVP：

- 用户可以发起一个 Agent 任务
- 后端可以按固定流程执行多 Agent 步骤
- 后端可以调用工具并记录日志
- 后端可以保存任务轨迹和记忆
- 后端可以返回任务详情、步骤状态、工具调用结果、最终产出
- 前端暂时不做复杂改造，只保留联调入口

### 1.3 推荐开发顺序

1. 数据模型与数据库表
2. DTO、Mapper、Repository
3. Tool Registry 与基础工具
4. Memory Service
5. Trace / Event Service
6. Orchestrator 编排器
7. Controller 接口
8. 测试
9. 前端联调

---

## 2. 现有后端基础

项目里已经有可复用的基础能力：

- `backend/src/main/java/com/hejulian/blog/rag/application/RagApplicationService.java`
- `backend/src/main/java/com/hejulian/blog/controller/RagController.java`
- `backend/src/main/java/com/hejulian/blog/dto/RagDtos.java`
- `backend/src/main/java/com/hejulian/blog/rag/domain/port/*`
- `backend/src/main/java/com/hejulian/blog/rag/domain/service/*`
- `backend/src/main/java/com/hejulian/blog/mapper/*`
- `backend/src/main/resources/mapper/*`
- `sql/blog_mysql_init.sql`
- `backend/src/main/java/com/hejulian/blog/service/AuthService.java`
- `backend/src/main/java/com/hejulian/blog/common/CacheNames.java`

### 2.1 现有能力可直接复用的地方

- RAG 检索、引用、历史、会话、反馈
- JWT 登录与用户隔离
- Redis 缓存
- MyBatis 持久化
- SSE 流式返回
- 后台文章 / 分类 / 标签 / 评论体系

### 2.2 不建议一上来改的地方

- 不要直接重构现有 RAG 主链路
- 不要把 Agent 逻辑硬塞进 `RagApplicationService`
- 不要先做复杂动态多 Agent 市场化框架
- 不要先做前端大改

---

## 3. 后端目标架构

建议新增独立的 `agent` 模块，避免与现有 `rag` 模块耦合过深：

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
  │   ├─ tool/
  │   ├─ cache/
  │   └─ llm/
  ├─ dto/
  └─ mapper/
```

### 3.1 核心服务职责

- `AgentTaskApplicationService`：任务创建、查询、取消、重试
- `AgentOrchestratorService`：多 Agent 顺序编排
- `AgentToolService`：工具注册、参数校验、权限校验、执行记录
- `AgentMemoryService`：记忆抽取、召回、写入、删除、pin
- `AgentTraceService`：轨迹事件、步骤日志、回放视图
- `AgentEvalService`：反馈与评估统计

---

## 4. 后端详细开发流程

## 4.1 第 1 步：先定数据表

这一步最重要，因为后续 DTO、Mapper、Service 都依赖它。

### 4.1.1 建议先新增的表

1. `agent_task`
2. `agent_task_step`
3. `agent_task_event`
4. `agent_tool_call`
5. `agent_memory`
6. `agent_memory_hit`
7. `agent_eval_record`

### 4.1.2 最小必需字段

#### `agent_task`

- `id`
- `user_id`
- `session_id`
- `task_type`
- `title`
- `goal`
- `status`
- `execution_mode`
- `search_scope`
- `allow_draft_write`
- `current_step`
- `final_output_summary`
- `error_message`
- `started_at`
- `completed_at`
- `created_at`
- `updated_at`

#### `agent_task_step`

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

#### `agent_task_event`

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

#### `agent_tool_call`

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

#### `agent_memory`

- `id`
- `user_id`
- `memory_scope`
- `topic_key`
- `memory_type`
- `content`
- `content_summary`
- `confidence_score`
- `source_type`
- `source_ref_id`
- `is_pinned`
- `is_deleted`
- `last_hit_at`
- `created_at`
- `updated_at`

#### `agent_memory_hit`

- `id`
- `task_id`
- `memory_id`
- `hit_reason`
- `used_in_step`
- `created_at`

#### `agent_eval_record`

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

### 4.1.3 落库位置

- `sql/blog_mysql_init.sql`
- 如需兼容已有用户初始化逻辑，再考虑独立初始化器

### 4.1.4 验收标准

- 数据库可以成功初始化
- 表字段能支持任务状态流转、工具日志、记忆写入、回放
- 表结构足够表达 MVP，不追求一次做完所有扩展字段

---

## 4.2 第 2 步：定义 DTO 和枚举

建议在 `backend/src/main/java/com/hejulian/blog/agent/dto/` 下新增请求与响应对象。

### 4.2.1 建议先定义的请求对象

- `AgentTaskCreateRequest`
- `AgentTaskQueryRequest`
- `AgentTaskCancelRequest`
- `AgentTaskRetryRequest`
- `AgentTaskSaveDraftRequest`
- `AgentMemoryUpdateRequest`
- `AgentMemoryDeleteRequest`
- `AgentMemoryPinRequest`

### 4.2.2 建议先定义的响应对象

- `AgentTaskResponse`
- `AgentTaskDetailResponse`
- `AgentTaskListResponse`
- `AgentTaskStepResponse`
- `AgentTaskEventResponse`
- `AgentToolCallResponse`
- `AgentMemoryResponse`
- `AgentEvalSummaryResponse`

### 4.2.3 建议先定义的枚举

- `TaskStatus`
- `TaskType`
- `ExecutionMode`
- `SearchScope`
- `AgentRole`
- `ToolPermissionLevel`
- `MemoryScope`
- `MemoryType`
- `EventType`

### 4.2.4 设计原则

- 请求对象必须便于前端直接提交
- 响应对象必须便于前端直接渲染
- 状态字段尽量用枚举字符串，不要混用自由文本
- 结果中要保留足够的 trace 信息，但不要暴露敏感 prompt 原文

---

## 4.3 第 3 步：实现 Mapper 和 Repository

### 4.3.1 先做哪些 Mapper

建议按以下顺序：

1. `AgentTaskMapper`
2. `AgentTaskStepMapper`
3. `AgentTaskEventMapper`
4. `AgentToolCallMapper`
5. `AgentMemoryMapper`
6. `AgentMemoryHitMapper`
7. `AgentEvalRecordMapper`

### 4.3.2 对应 XML

- `backend/src/main/resources/mapper/AgentTaskMapper.xml`
- `backend/src/main/resources/mapper/AgentTaskStepMapper.xml`
- `backend/src/main/resources/mapper/AgentTaskEventMapper.xml`
- `backend/src/main/resources/mapper/AgentToolCallMapper.xml`
- `backend/src/main/resources/mapper/AgentMemoryMapper.xml`
- `backend/src/main/resources/mapper/AgentMemoryHitMapper.xml`
- `backend/src/main/resources/mapper/AgentEvalRecordMapper.xml`

### 4.3.3 建议的持久化风格

- 任务主表负责当前状态
- 步骤表负责阶段拆分
- 事件表负责时间线回放
- 工具调用表负责审计
- 记忆表负责长期存储
- 命中表负责追踪“为什么记住了这条信息”

### 4.3.4 验收标准

- 能保存任务
- 能按任务 ID 查询详情
- 能按用户 ID 查询列表
- 能追加步骤、事件、工具调用、记忆记录
- 能做软删、取消、重试等操作的基础持久化

---

## 4.4 第 4 步：实现 Tool Registry

这是 Agent 后端里最关键的基础层之一。

### 4.4.1 目标

把“能被 Agent 调用的能力”统一成标准工具，而不是散落在 service 里。

### 4.4.2 建议接口

- `AgentTool`
  - `name()`
  - `description()`
  - `inputSchema()`
  - `permissionLevel()`
  - `execute(ToolContext, JsonNode args)`

### 4.4.3 第一批建议工具

#### 只读工具

- `search_site_posts`
- `get_post_detail`
- `list_categories_tags`
- `search_knowledge_chunks`
- `web_search`
- `get_user_memory_summary`

#### 写入工具

- `save_post_draft`
- `save_task_note`
- `pin_memory_item`

### 4.4.4 工具实现原则

- 每个工具必须做 schema 校验
- 每个工具必须做权限校验
- 每个工具必须记录调用日志
- 每个工具必须能独立失败、独立重试
- 写工具必须留审计痕迹

### 4.4.5 建议文件位置

```text
backend/src/main/java/com/hejulian/blog/agent/tool/
backend/src/main/java/com/hejulian/blog/agent/infrastructure/tool/
```

### 4.4.6 验收标准

- 能通过统一入口调用工具
- 能输出标准化结果
- 能落库工具调用日志
- 能限制普通用户不能执行高危写操作

---

## 4.5 第 5 步：实现 Memory Service

### 4.5.1 目标

让系统记住“用户稳定偏好”和“任务背景”，而不是只保留短期上下文。

### 4.5.2 三层记忆落地顺序

#### 第一层：短期记忆

- 任务执行中的上下文
- 当前会话最近消息
- 临时中间结论

存储优先级：Redis > MySQL 摘要

#### 第二层：长期记忆

- 用户偏好
- 输出风格
- 常用技术栈
- 明确确认的重要事实

存储优先级：MySQL

#### 第三层：情景记忆

- 某个长期任务的历史进展
- 上一次未完成的工作内容
- 某次研究任务的结论

存储优先级：MySQL + Redis 热缓存

### 4.5.3 记忆抽取流程

1. 用户任务结束后，Memory Extractor 分析结果
2. 判断哪些信息值得写入长期记忆
3. 给每条记忆打置信度
4. 低置信度内容默认不升级为长期记忆
5. 记忆写入后记录 hit / source / confidence

### 4.5.4 记忆召回流程

1. 从 Redis 取当前会话短期记忆
2. 从 MySQL 取用户高置信度长期记忆
3. 取当前主题相关的情景记忆
4. 做摘要压缩后交给 Planner

### 4.5.5 验收标准

- 同一用户第二次下任务时，系统能复用偏好
- 用户可查看、删除、pin 自己的记忆
- 任务轨迹能显示命中了哪些记忆

---

## 4.6 第 6 步：实现 Trace / Event Service

### 4.6.1 目标

让每次 Agent 执行都可以被解释、回放、排查。

### 4.6.2 需要记录的事件

- 任务创建
- 任务开始
- Planner 输出计划
- Research 调用工具
- Writer 生成正文
- Reviewer 提出问题
- 重试或降级
- 任务完成或失败

### 4.6.3 记录建议

- 事件只保存摘要，不要默认存整段 prompt 原文
- 对管理员可开放更高粒度日志
- 事件时间线要能按顺序还原执行过程

### 4.6.4 验收标准

- 后台可以看到完整时间线
- 每个步骤有开始、结束、耗时、状态
- 工具调用和记忆命中都能对应到具体步骤

---

## 4.7 第 7 步：实现 Orchestrator

### 4.7.1 推荐编排方式

第一期建议固定顺序：

```text
Planner -> Research -> Writer -> Reviewer -> Publisher(optional)
```

不要一开始做任意图拓扑，否则调试成本太高。

### 4.7.2 执行流程

1. 创建任务记录，状态设为 `PENDING`
2. 生成初始步骤列表
3. 召回记忆
4. 执行 Planner
5. 根据 Planner 的输出执行 Research
6. Research 完成后交给 Writer
7. Writer 完成后交给 Reviewer
8. Reviewer 判断是否需要重写或补证据
9. 若允许写草稿，调用 Publisher
10. 汇总最终结果并更新任务状态

### 4.7.3 失败恢复策略

- 工具失败：重试 1~2 次
- Research 证据不足：回到 Planner 重新补检索
- Writer 输出不合格：交给 Reviewer 触发重写
- 多 Agent 失败：降级为单 Agent + RAG
- 写草稿失败：保留文本结果，提示用户手动保存

### 4.7.4 验收标准

- 一个任务能完整跑通
- 失败会留下可定位日志
- 能降级而不是直接中断整个系统

---

## 4.8 第 8 步：实现 Controller 接口

### 4.8.1 建议新增控制器

- `backend/src/main/java/com/hejulian/blog/agent/controller/AgentTaskController.java`
- `backend/src/main/java/com/hejulian/blog/agent/controller/AgentMemoryController.java`
- `backend/src/main/java/com/hejulian/blog/agent/controller/admin/AdminAgentController.java`

### 4.8.2 用户侧接口建议

- `POST /api/agent/tasks`
- `GET /api/agent/tasks`
- `GET /api/agent/tasks/{taskId}`
- `POST /api/agent/tasks/{taskId}/cancel`
- `POST /api/agent/tasks/{taskId}/retry`
- `GET /api/agent/tasks/{taskId}/stream`
- `POST /api/agent/tasks/{taskId}/save-draft`
- `GET /api/agent/memories/me`
- `DELETE /api/agent/memories/{id}`
- `POST /api/agent/memories/{id}/pin`

### 4.8.3 管理员侧接口建议

- `GET /api/admin/agent/tasks`
- `GET /api/admin/agent/tasks/{taskId}/trace`
- `GET /api/admin/agent/tool-calls`
- `GET /api/admin/agent/memory-hits`
- `GET /api/admin/agent/evals/summary`

### 4.8.4 接口设计原则

- 用户接口只返回与自己相关的数据
- 管理员接口才能看全局任务和更细日志
- 所有写操作都必须鉴权
- 所有列表接口都要分页

---

## 4.9 第 9 步：接入缓存

### 4.9.1 Redis 适合存什么

- 任务执行中的短期状态
- 当前会话热点数据
- 用户近期记忆摘要
- 任务流式状态

### 4.9.2 不建议只放 Redis 的内容

- 长期记忆原文
- 审计轨迹原始日志
- 草稿写入结果
- 可追责的评估记录

### 4.9.3 缓存原则

- 热数据短缓存
- 结构化数据 MySQL 为准
- 写操作后要做缓存失效

---

## 4.10 第 10 步：测试与验证

### 4.10.1 必测范围

- 任务创建
- 任务执行流程
- 工具调用权限
- 记忆写入与召回
- 任务失败重试
- 任务取消
- 轨迹查询
- 草稿写入

### 4.10.2 测试类型

- 单元测试
- Service 层测试
- Controller 接口测试
- 数据库持久化测试
- 联调测试

### 4.10.3 验收标准

- 后端能独立跑通整个 MVP
- 无需前端也能通过接口验证流程
- 每个核心能力都有可回放数据

---

## 5. 建议的后端开发任务拆分

### 阶段 1：最小可运行后端

- 建表
- DTO
- Mapper
- 任务 Service
- Orchestrator 固定流程
- 3~4 个只读工具
- 基础 Controller

### 阶段 2：记忆系统

- 记忆表
- 记忆抽取
- 记忆召回
- 记忆管理接口

### 阶段 3：可观测性

- 事件时间线
- 工具调用日志
- 任务回放
- 管理员接口

### 阶段 4：写入闭环

- 草稿保存工具
- 草稿写入接口
- 审批与权限控制

---

## 6. 前端开发流程（放在后端完成之后）

前端不要提前重做，等后端接口稳定后再接入。

### 6.1 前端第一步

- `KnowledgeView.vue` 增加任务模式开关
- 增加任务创建表单
- 增加任务状态展示卡

### 6.2 前端第二步

- 接任务详情和步骤列表接口
- 显示工具调用日志
- 显示记忆命中信息

### 6.3 前端第三步

- 接 SSE 或轮询任务流式状态
- 支持中断、重试、继续任务

### 6.4 前端第四步

- 新增后台 Agent Ops 页面
- 新增记忆管理页面
- 新增任务回放页面

### 6.5 前端联调原则

- 前端只消费稳定 DTO
- 不把业务规则写进 Vue
- 所有流程判断都放后端

---

## 7. 推荐你现在就按这个顺序做

1. 先建 Agent 表
2. 再写 DTO 和 Mapper
3. 再做 Tool Registry
4. 再做 Memory Service
5. 再做 Orchestrator
6. 再补 Controller
7. 再写测试
8. 最后再改前端

---

## 8. 关键提醒

- 先做固定流程，不要先做“万能 agent 平台”
- 先跑通一个任务，不要一开始做很多任务类型
- 先只读工具，写工具放到后面
- 先保存摘要，不要一开始存太多原始 prompt
- 先后端稳定，再做前端美化

---

## 9. 推荐的首个后端 MVP

建议 MVP 就做这一条链路：

**用户提交博客创作任务 -> 后端多 Agent 编排 -> 检索站内内容 -> 生成初稿 -> reviewer 校验 -> 返回最终结果 -> 记录任务轨迹**

这条链路最能体现：

- 多 Agent 协同
- 工具调用
- 记忆管理
- 可观测性
- 人机协同

如果你要继续，我下一步可以直接给你补一份：

- 后端类图/模块图
- 数据库建表 SQL 草案
- 具体接口定义草案
- 第一阶段任务清单（按天拆分）
