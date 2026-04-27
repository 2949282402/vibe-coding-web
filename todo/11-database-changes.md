# 第一阶段数据库变更方案

## 一、目标

定义第一阶段需要新增或调整的数据库结构，支撑：

- RAG 问答统计与运营分析
- Agent 草稿审核与人工发布流程

## 二、RAG 侧变更

## 1. 新增 `rag_query_event` 表

### 用途

用于记录每次问答请求的关键埋点，支撑知识库运营后台与质量分析。

### 建议字段

```sql
CREATE TABLE rag_query_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  session_id VARCHAR(64) NOT NULL,
  question VARCHAR(1000) NOT NULL,
  answer_mode VARCHAR(32) NOT NULL,
  search_mode VARCHAR(32) NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  retrieval_hit_count INT NOT NULL DEFAULT 0,
  used_web_search TINYINT(1) NOT NULL DEFAULT 0,
  latency_ms INT NOT NULL DEFAULT 0,
  success TINYINT(1) NOT NULL DEFAULT 1,
  helpful TINYINT(1) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 索引建议

```sql
CREATE INDEX idx_rag_query_event_user_id ON rag_query_event(user_id);
CREATE INDEX idx_rag_query_event_created_at ON rag_query_event(created_at);
CREATE INDEX idx_rag_query_event_answer_mode ON rag_query_event(answer_mode);
CREATE INDEX idx_rag_query_event_search_mode ON rag_query_event(search_mode);
CREATE INDEX idx_rag_query_event_source_type ON rag_query_event(source_type);
CREATE INDEX idx_rag_query_event_session_id ON rag_query_event(session_id);
```

### 字段说明

- `answer_mode`：`RAG | ASK`
- `search_mode`：`LOCAL_ONLY | WEB_ONLY | LOCAL_AND_WEB`
- `source_type`：`local | web | mixed | none`
- `helpful`：初始可为空，待用户反馈后更新

## 2. 写入时机建议

建议在以下时机写入或更新：

- 普通问答完成后写入
- SSE `done` 后写入
- SSE `error` 时写入失败记录
- 用户提交 helpful/not helpful 后更新对应记录

## 三、Agent 侧变更

## 1. 优先方案：扩展现有 Agent 任务或文章草稿结构

若当前项目已有 Agent 任务表，建议优先在现有结构上补字段，而不是新起一套完全独立表。

### 建议新增字段

- `review_status`：`DRAFT_READY | REVIEW_REJECTED | PUBLISHED`
- `reviewed_by`
- `reviewed_at`
- `reject_reason`
- `draft_post_id` 或复用现有 `post_id`

### 示例

```sql
ALTER TABLE agent_task
  ADD COLUMN review_status VARCHAR(32) NULL,
  ADD COLUMN reviewed_by BIGINT NULL,
  ADD COLUMN reviewed_at DATETIME NULL,
  ADD COLUMN reject_reason VARCHAR(1000) NULL;
```

## 2. 备选方案：新增 `agent_draft_review` 表

如果不希望把审核信息直接塞进任务表，可拆独立审核表：

```sql
CREATE TABLE agent_draft_review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  post_id BIGINT NOT NULL,
  review_status VARCHAR(32) NOT NULL,
  reviewed_by BIGINT NULL,
  reviewed_at DATETIME NULL,
  reject_reason VARCHAR(1000) NULL,
  editor_note VARCHAR(1000) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 取舍建议

- 若目标是快速落地，优先扩任务表
- 若后续要支持多轮审核、审核历史，优先独立审核表

第一阶段建议选择“扩任务表”，因为改动更小。

## 四、状态设计建议

## 1. Agent 任务状态

建议保留主状态流，同时补审核语义：

- 主执行状态：`PENDING | RUNNING | FAILED | CANCELLED | COMPLETED`
- 审核状态：`DRAFT_READY | REVIEW_REJECTED | PUBLISHED`

不建议把所有语义都塞进一个状态字段，否则会混淆“执行完成”和“发布完成”。

## 2. 数据一致性建议

- 任务执行完成时，仅更新为“已生成草稿”。
- 审核通过发布时，单独更新审核状态和发布时间。
- 驳回时不改动草稿正文，只更新审核状态与原因。

## 五、SQL 初始化与兼容升级

## 1. 初始化脚本

需要更新：

- `sql/blog_mysql_init.sql`

## 2. 兼容升级

如果项目已存在 schema initializer，建议同步补以下逻辑：

- 表不存在则创建 `rag_query_event`
- 字段不存在则补充 Agent 审核字段
- 索引不存在则补齐索引

这样可以兼容已有环境，而不只适用于全新建库。

## 六、MyBatis 层改动建议

需同步更新：

- `entity`
- `mapper`
- `mapper XML`
- `service`
- 如有 DTO 转换层，也要同步补字段

RAG 侧至少需要：

- `RagQueryEventEntity`
- `RagQueryEventMapper`
- 对应 XML

Agent 侧至少需要：

- Agent 任务 entity 增加审核字段，或新增审核 entity
- 审核查询与状态更新 SQL

## 七、索引与性能建议

第一阶段统计场景主要是：

- 按时间统计问答量
- 按模式统计使用分布
- 查无命中问题
- 查低帮助率问题

因此索引优先级：

1. `created_at`
2. `answer_mode`
3. `search_mode`
4. `source_type`
5. `session_id`

如果后续分析量变大，再考虑汇总表或离线统计。

## 八、缓存影响

### RAG 侧

- `rag_query_event` 属于统计数据，初期可不缓存写操作。
- 统计查询若频繁，可对后台总览做短 TTL 缓存。

### Agent 侧

- 草稿审核通过发布后，必须触发首页、文章列表、详情相关缓存失效。
- 驳回不应触发前台内容缓存刷新。

## 九、验收标准

- 能记录一次问答的模式、来源、耗时、是否 helpful。
- Agent 任务完成后可进入草稿审核，而不是直接发布。
- 数据结构对现有功能兼容，不破坏旧任务和旧问答流程。