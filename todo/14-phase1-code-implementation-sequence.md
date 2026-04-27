# 第一阶段代码实施顺序

## 一、目标

把第一阶段方案进一步压缩成“真正开始改代码时的执行顺序”，避免多人并行时互相阻塞，也避免先改 UI 后发现接口未定、先改数据库后发现状态设计不稳。

## 二、实施原则

- 先扩展、后切换：优先新增兼容能力，再逐步替换旧展示与旧流程。
- 先后端契约、再前端消费：避免前端先写死结构。
- 先状态机、再页面操作：Agent 草稿流必须先定义清楚状态和接口。
- 先主链路、后统计看板：先保证用户问答和管理员审核能走通，再补运营能力。

## 三、推荐实施顺序

## Step 1：确认后端契约与状态设计

### 目标

先冻结第一阶段的接口字段与状态机，避免开发过程中反复改协议。

### 需要确认的内容

- `AskResponse` 新字段最终命名是否固定：
  - `sourceType`
  - `retrievalHitCount`
  - `usedWebSearch`
  - `latencyMs`
  - `confidenceLevel`
  - `knowledgeUpdatedAt`
- Agent 是否采用“双状态”：执行状态 + 审核状态
- Agent 草稿审核接口是否按 `approve/reject` 拆分

### 关联文档

- `todo/10-api-contracts.md`
- `todo/11-database-changes.md`

## Step 2：后端先扩展 RAG / Ask 返回结构

### 先改文件

- `backend/src/main/java/com/hejulian/blog/dto/RagDtos.java`
- `backend/src/main/java/com/hejulian/blog/controller/RagController.java`
- `backend/src/main/java/com/hejulian/blog/rag/application/RagApplicationService.java`

### 实施内容

- 新增 `AskResponse` 字段，但不删除旧字段。
- 保证普通问答与 SSE `done` 最终结构一致。
- Ask 模式固定返回 `sourceType=none`。

### 为什么先做

这是前端重构 `/knowledge` 页面之前的基础，否则前端只能写占位逻辑。

## Step 3：后端补来源与可信度计算逻辑

### 重点文件

- `backend/src/main/java/com/hejulian/blog/rag/application/RagApplicationService.java`
- `backend/src/main/java/com/hejulian/blog/rag/application/KnowledgeRetrievalService.java`
- `backend/src/main/java/com/hejulian/blog/rag/application/CitationGuardService.java`

### 实施内容

- 计算 `sourceType`
- 计算 `retrievalHitCount`
- 记录 `usedWebSearch`
- 计算 `latencyMs`
- 用规则法生成 `confidenceLevel`

### 注意点

- 先用规则法，不引入复杂评分系统。
- `WEB_ONLY`、弱召回、无引用场景要能清晰区分。

## Step 4：前端接入新问答字段，但先不重构页面

### 先改文件

- `frontend/src/api/blog.js`
- `frontend/src/views/KnowledgeView.vue`

### 实施内容

- 前端先兼容接收新增字段。
- 在不大改布局的前提下，先确保数据能进入状态树。
- 对空值容错，避免后端部分场景未返回时页面报错。

### 为什么这么做

先完成“数据接通”，再做 UI 重构，风险更低。

## Step 5：重构 `/knowledge` 页面结构

### 重点文件

- `frontend/src/views/KnowledgeView.vue`
- `frontend/src/styles/global.css`

### 实施内容

- 移除前台 `Agent` 一级入口。
- Ask 降级到高级设置。
- 增加首页空态、示例问题、能力说明。
- 增加元信息展示区。

### 注意点

- 不要同时重写 SSE 逻辑。
- 页面结构改造和消息流逻辑分层处理。

## Step 6：后端切换 Agent 为草稿流

### 重点文件

- `backend/src/main/java/com/hejulian/blog/agent/application/AgentTaskApplicationService.java`
- `backend/src/main/java/com/hejulian/blog/agent/application/AgentOrchestratorService.java`
- `backend/src/main/java/com/hejulian/blog/service/AdminBlogService.java`

### 实施内容

- 任务完成后默认落草稿。
- 阻断自动直发路径。
- 输出 `DRAFT_READY` 或等价审核准备状态。

### 注意点

- 保留现有发布服务，不要推翻。
- 重点是增加人工审核层。

## Step 7：补数据库字段或表结构

### 重点文件

- `sql/blog_mysql_init.sql`
- Agent 相关 entity / mapper / XML
- 如存在 schema initializer，一并修改

### 实施内容

- 为 Agent 审核补字段或审核表。
- 新增 `rag_query_event` 表。
- 补索引和兼容升级逻辑。

### 为什么放在这里

- Agent 草稿流真正落库前，状态结构必须稳定。
- 统计表可与后台看板并行推进，但不应阻断主链路。

## Step 8：新增 Agent 审核接口

### 重点文件

- Agent 相关 controller / dto / service / mapper / XML

### 实施内容

- 草稿列表
- 草稿详情
- 审核通过发布
- 驳回草稿

### 验收目标

- 管理员可完整走通 `DRAFT_READY -> PUBLISHED`
- 普通用户无法访问接口

## Step 9：前端新增后台草稿审核页

### 重点文件

- `frontend/src/views/admin/AgentDraftReviewView.vue`
- `frontend/src/api/admin.js`
- `frontend/src/router/index.js`

### 实施内容

- 展示草稿列表
- 支持预览、发布、驳回
- 支持跳转任务轨迹

## Step 10：补问答埋点与知识库运营接口

### 重点文件

- `sql/blog_mysql_init.sql`
- RAG 统计相关 entity / mapper / service / controller

### 实施内容

- 写入 `rag_query_event`
- 提供 overview / query-stats / unanswered-top / low-helpful-top

### 注意点

- 埋点不能影响主问答性能和稳定性。
- 初期统计写入应保持简单、低耦合。

## Step 11：前端新增知识库运营页

### 重点文件

- `frontend/src/views/admin/KnowledgeOpsView.vue`
- `frontend/src/api/admin.js`
- `frontend/src/router/index.js`

### 实施内容

- 展示问答量、帮助率、平均耗时、Web Search 占比
- 展示无命中问题和低帮助率问题

## 四、建议提交粒度

建议按以下粒度提交代码，而不是一口气改完：

1. RAG / Ask 响应字段扩展
2. `/knowledge` 页面数据接入与结构重构
3. Agent 草稿流后端改造
4. Agent 草稿审核后台页面
5. 问答埋点与知识库运营后台

这样每一步都能独立验证和回滚。

## 五、并行边界

可以并行：

- 后端 RAG 字段扩展 与 前端页面结构设计
- 后端 Agent 审核接口 与 前端审核页搭建
- 数据库埋点表 与 运营后台静态页面搭建

不建议并行：

- 未冻结 `AskResponse` 字段前，前后端同时写死逻辑
- 未稳定 Agent 状态机前，同时推进审核页和发布逻辑

## 六、完成定义

第一阶段代码实施完成，至少应满足：

- `/knowledge` 只服务普通用户问答
- RAG / Ask 回答具备清晰元信息
- Agent 生成内容默认进入草稿审核
- 管理员可在后台审核并发布草稿
- 统计接口与基础运营页可用