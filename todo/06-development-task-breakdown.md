# 开发任务拆解清单

## 一、目标

将前期产品与技术方案拆成可直接执行的开发任务，便于按前端、后端、数据库、测试、联调进行排期与分工。

## 二、P0 任务拆解

## 任务组 A：前台 `/knowledge` 场景收敛

### 前端任务

- 调整 `frontend/src/views/KnowledgeView.vue` 的模式呈现，前台不再展示 `Agent` 一级入口。
- 将 `Ask` 从主模式降级为高级选项或次级入口。
- 将搜索范围 `LOCAL_ONLY / LOCAL_AND_WEB / WEB_ONLY` 从主控件改为渐进展开。
- 增加问答首页态：示例问题、能力说明、最近会话入口。
- 增加回答元信息区域：来源类型、可信度、耗时、命中来源数。

### 后端任务

- 保持现有 `RAG` 与 `Ask` 接口兼容，避免前端切换期间出现协议断裂。
- 审查 `/api/agent/**` 是否只在后台路由和管理员入口暴露。

### 验收标准

- 普通用户在 `/knowledge` 中看不到 `Agent` 模式。
- Ask 仍可用，但不再和 RAG 平铺为同级主入口。
- 页面首次进入不再是直接空白聊天态。

## 任务组 B：RAG 回答可解释性增强

### 前端任务

- 在回答卡片中展示 `sourceType`、`confidenceLevel`、`latencyMs`、`retrievalHitCount`。
- 对 `web`、`mixed`、`none` 三类来源展示不同提示文案。
- 优化来源区域 UI，使引用与答案主内容形成稳定层级。
- 为低可信度结果增加警示提示。

### 后端任务

- 扩展 `RagDtos.AskResponse` 返回字段。
- 在 `RagApplicationService.java` 中统一计算来源类型、命中数、耗时。
- 明确 Ask 模式返回 `sourceType=none`，避免与 RAG 语义混淆。
- 若为 `WEB_ONLY` 或弱召回，输出可解释标签而不是模糊结果。

### 数据任务

- 确定 `confidenceLevel` 的计算规则，可先用规则法，后续再优化。
- 增加问答结果元信息的日志输出，便于初期验证。

### 验收标准

- 每次回答都能说明主要依赖站内、网络还是混合信息。
- Ask 结果不会伪装成站内知识回答。
- 用户可以明显区分“可信回答”和“建议核验回答”。

## 任务组 C：Agent 草稿流改造

### 前端任务

- 后台新增草稿审核页面，例如 `frontend/src/views/admin/AgentDraftReviewView.vue`。
- 在 `AgentOpsView.vue` 中区分任务执行结果与发布状态。
- 增加“审核通过发布”“驳回草稿”“继续编辑”操作入口。

### 后端任务

- 在 `AgentTaskApplicationService.java` 中将任务终态从“直接发布”改为“草稿就绪”。
- 在 `AgentOrchestratorService.java` 中保证最终产物写入草稿，而不是直接走发布。
- 在 `AdminBlogService.java` 上补一层人工确认发布动作。
- 新增或扩展任务状态：`DRAFT_READY`、`REVIEW_REJECTED`、`PUBLISHED`。

### 数据库任务

- 扩展 Agent 任务或文章草稿相关字段，记录审核状态、审核人、审核时间、驳回原因。
- 更新 MyBatis entity / mapper / XML / SQL 初始化脚本。

### 验收标准

- Agent 任务完成后不会直接出现在前台文章列表。
- 管理员可在后台明确看到草稿、审核状态与发布动作。
- 发布行为必须由人工触发。

## 任务组 D：知识库运营后台 MVP

### 前端任务

- 新增 `frontend/src/views/admin/KnowledgeOpsView.vue`。
- 展示问答量、帮助率、平均耗时、无命中问题、Web Search 占比等基础卡片。
- 支持时间范围与基础筛选。

### 后端任务

- 新增运营统计接口：总览、问题统计、低质量问题列表。
- 汇总 `rag_query_event` 与现有反馈数据。

### 数据库任务

- 新增 `rag_query_event` 表。
- 建立最基本索引：`user_id`、`created_at`、`answer_mode`、`search_mode`。

### 验收标准

- 管理员能知道问答是否被真实使用。
- 能定位问答质量差的问题，而不是只看主观感觉。

## 三、横向任务

## 测试任务

- 后端补充 DTO、Service、Controller 层测试，重点覆盖新增状态和字段。
- 前端至少执行 `npm run build` 并手工验证问答模式切换、SSE、后台审核流。
- 检查 Agent、RAG SSE 在 nginx 下是否仍然无缓冲。

## 联调任务

- 前后端统一 `AskResponse` 字段契约。
- 联调后台 Agent 草稿状态机。
- 联调统计接口与后台看板展示。

## 运维任务

- 若涉及 SSE 或后台页面代理调整，检查 `frontend/nginx/default.conf.template`。
- 若新增表结构，补齐初始化脚本和兼容升级逻辑。

## 四、建议分工

- 前端：`KnowledgeView.vue`、后台运营页、草稿审核页、数据展示
- 后端：RAG 响应增强、Agent 草稿流、统计接口
- 数据库：新增埋点表、任务状态字段、索引与初始化脚本
- 测试/联调：SSE、权限、状态流转、缓存失效验证

## 五、交付顺序

1. 先做 `/knowledge` 场景收敛
2. 再做 RAG 可解释性
3. 同步推进 Agent 草稿流
4. 最后补运营后台 MVP