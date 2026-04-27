# 技术方案

## 一、目标

围绕当前系统，落地一个“知识中台 2.0”方案，核心目标如下：

- 前台只承载用户问答
- 后台独立承载 Agent 内容生产
- RAG 回答具备清晰的可解释性
- Agent 发布链路具备人工审核能力
- 系统具备可量化的运营指标

## 二、前端方案

### 1. 路由调整

建议新增或调整页面：

- 保留 `frontend/src/views/KnowledgeView.vue`，但只服务普通用户问答
- 新增后台页面：`frontend/src/views/admin/KnowledgeOpsView.vue`
- 保留或重构：`frontend/src/views/admin/AgentOpsView.vue`
- 新增后台页面：`frontend/src/views/admin/AgentDraftReviewView.vue`

建议路由：

- `/knowledge`：问答主入口
- `/admin/knowledge-ops`：知识库运营
- `/admin/agent`：Agent 工作台
- `/admin/agent/drafts`：Agent 草稿审核

### 2. 问答页面改造

对 `KnowledgeView.vue` 做结构重构：

- 增加首页态：示例问题、能力介绍、最近会话
- 默认只强调“基于本站知识提问”
- 将 `Ask` 放到高级选项中
- 将搜索模式折叠到高级设置，而非主控件
- 增加回答元信息栏：来源类型、可信度、耗时、命中来源数

### 3. 回答数据结构扩展

建议扩展 `RagDtos.AskResponse` 字段：

- `sourceType`: `local | web | mixed | none`
- `retrievalHitCount`: 命中来源数
- `usedWebSearch`: 是否启用网络搜索
- `latencyMs`: 总耗时
- `confidenceLevel`: `high | medium | low`
- `knowledgeUpdatedAt`: 主要依据内容的更新时间

前端据此渲染“可信度卡片”和“回答说明”。

## 三、后端方案

### 1. RAG 服务增强

修改链路：

- `RagController.java`
- `RagDtos.java`
- `RagApplicationService.java`
- `KnowledgeRetrievalService.java`
- `CitationGuardService.java`

重点改动：

- 在问答返回中增加来源分类与质量元信息
- 对无召回、弱召回、纯 Web Search 等情况输出明确标签
- Ask 模式继续绕过本地检索，但返回 `sourceType=none`

### 2. 新增问答埋点表

建议新增表：`rag_query_event`

核心字段建议：

- `id`
- `user_id`
- `session_id`
- `question`
- `answer_mode`
- `search_mode`
- `source_type`
- `retrieval_hit_count`
- `used_web_search`
- `latency_ms`
- `success`
- `helpful`
- `created_at`

用途：

- 统计问答量
- 分析无命中问题
- 统计低帮助率问题
- 分析 Web Search 依赖度
- 分析性能瓶颈

### 3. Agent 发布流程改造

修改链路：

- `AgentTaskApplicationService.java`
- `AgentOrchestratorService.java`
- `AdminBlogService.java`
- Agent 相关 DTO / entity / mapper / XML

建议流程：

1. 创建 Agent 任务
2. 执行 planner / researcher / writer / reviewer
3. 生成最终文章草稿
4. 保存为后台草稿，不直接发布
5. 管理员人工审核
6. 审核通过后调用发布逻辑

建议新增任务状态：

- `PENDING`
- `RUNNING`
- `DRAFT_READY`
- `REVIEW_REJECTED`
- `PUBLISHED`
- `FAILED`
- `CANCELLED`

### 4. 后台运营接口

建议新增：

- `GET /api/admin/knowledge/overview`
- `GET /api/admin/knowledge/query-stats`
- `GET /api/admin/knowledge/unanswered-top`
- `GET /api/admin/knowledge/low-helpful-top`
- `GET /api/admin/agent/drafts`
- `POST /api/admin/agent/drafts/{id}/approve`
- `POST /api/admin/agent/drafts/{id}/reject`

## 四、数据库与缓存方案

### 1. 数据库改动

建议新增或扩展：

- `rag_query_event`
- `agent_draft_review`，或在现有 agent task / post 草稿表上补审核字段

### 2. 缓存策略

以下场景要检查 Redis 缓存失效：

- Agent 草稿被人工发布后：首页、文章列表、文章详情缓存
- RAG 反馈提交后：问答反馈统计缓存
- 知识库重建后：会话上下文中的知识命中统计缓存

## 五、部署与运维注意事项

- 保持 SSE 路由的 nginx 非缓冲配置
- 若前端展示问答元信息，前后端返回契约要同步升级
- 若新增后台页面，需要同时更新前端路由和后台权限判断
- Docker 部署下如修改 nginx 配置，必须改 `frontend/nginx/default.conf.template`

## 六、MVP 范围

第一阶段不建议大规模重构检索算法，而是优先完成：

- 页面入口拆分
- 回答元信息补齐
- Agent 草稿流
- 运营后台基础指标

这是投入最小、收益最高的一组改动。