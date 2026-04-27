# 第一阶段详细落地方案

## 一、阶段目标

第一阶段只解决三个最高优先级问题：

- 前台问答场景混乱
- RAG 回答可信度表达不足
- Agent 自动发布风险过高

本阶段不追求大规模算法升级，也不追求复杂多 Agent 扩展，而是先把产品结构和交互链路做对。

## 二、范围定义

### 包含内容

- 从前台 `/knowledge` 中移除 `Agent` 主入口
- 将 `Ask` 从主模式降级为次级能力
- 增加回答元信息字段和前端展示
- 将 Agent 改为草稿流
- 新增后台草稿审核动作

### 暂不包含

- 检索算法重写
- 推荐系统
- 用户收藏/订阅
- 多 Agent 协同扩展
- 深度 BI 看板

## 三、页面与交互方案

## 1. `/knowledge` 页面重构

### 调整前

- 用户直接面对 `RAG / Ask / Agent` 三模式选择
- 搜索模式暴露较多
- 页面更偏技术调试视角

### 调整后

页面分为四块：

- 顶部价值说明：基于本站内容回答问题
- 示例问题区：降低首次提问门槛
- 会话区：保留当前聊天能力
- 高级选项：包含 Ask 与搜索范围

### 交互原则

- 默认路径必须简单，尽量一步提问
- 高级能力渐进展开，不干扰首次用户
- 对非站内来源回答必须明确提示

## 2. 回答卡片设计

建议新增以下展示区：

- 来源标签：站内 / 网络 / 混合 / 模型回答
- 可信度标签：高 / 中 / 低
- 响应耗时
- 命中文章数
- 主要依据文章及更新时间

### 提示文案策略

- `local + high`：优先展示“基于本站内容整理”
- `mixed + medium`：展示“结合站内与网络信息”
- `web + low`：展示“主要来自网络，建议人工核验”
- `none`：展示“本次回答未引用站内知识” 

## 3. Agent 审核流设计

Agent 完成后，不再直接发布，而是进入草稿审核页。

审核页应提供：

- 草稿标题
- 正文预览
- 生成摘要
- 来源摘要
- 执行轨迹入口
- 审核动作：通过发布 / 驳回 / 继续人工编辑

## 四、技术改造点

## 1. 前端改造点

重点文件：

- `frontend/src/views/KnowledgeView.vue`
- `frontend/src/api/blog.js`
- `frontend/src/views/admin/AgentOpsView.vue`
- 新增 `frontend/src/views/admin/AgentDraftReviewView.vue`
- 路由文件 `frontend/src/router/index.js`

前端实施顺序：

1. 先改 `/knowledge` 展示结构
2. 再接新增响应字段
3. 最后补后台草稿审核页

## 2. 后端改造点

重点文件：

- `backend/src/main/java/com/hejulian/blog/controller/RagController.java`
- `backend/src/main/java/com/hejulian/blog/dto/RagDtos.java`
- `backend/src/main/java/com/hejulian/blog/rag/application/RagApplicationService.java`
- `backend/src/main/java/com/hejulian/blog/agent/application/AgentTaskApplicationService.java`
- `backend/src/main/java/com/hejulian/blog/agent/application/AgentOrchestratorService.java`
- `backend/src/main/java/com/hejulian/blog/service/AdminBlogService.java`

后端实施顺序：

1. 扩展问答响应字段
2. 实现来源类型与可信度规则
3. 调整 Agent 任务终态
4. 增加审核发布接口

## 3. 数据库改造点

### RAG 侧

短期可以先不新增复杂结构，只确保日志和接口字段完整。
若要补埋点，优先增加 `rag_query_event`。

### Agent 侧

至少要补以下能力之一：

- 在 Agent 任务表增加审核状态字段
- 或新增草稿审核表，记录审核动作与原因

## 五、状态机建议

## 1. 问答结果可信度规则

第一阶段建议采用规则法：

- `high`：本地召回充分且引用完整
- `medium`：本地召回有限，或本地与网络混合
- `low`：纯网络搜索、无明确引用、或弱召回

这样足够稳定，也便于调试。

## 2. Agent 任务状态机

建议状态流转：

- `PENDING -> RUNNING -> DRAFT_READY`
- `DRAFT_READY -> PUBLISHED`
- `DRAFT_READY -> REVIEW_REJECTED`
- 任意执行中状态可转 `FAILED` 或 `CANCELLED`

禁止出现：

- `RUNNING -> PUBLISHED`
- `CANCELLED -> PUBLISHED`

## 六、验收方案

## 1. 产品验收

- 普通用户只能在前台看到问答产品，而不是写作工具。
- 用户能理解当前回答是否来自站内知识。
- 管理员不会因为 Agent 自动产出而误发文章。

## 2. 技术验收

- `AskResponse` 新字段前后端联调通过。
- Agent 草稿状态能够被正确展示和流转。
- SSE 不因页面或 nginx 调整而退化。
- 缓存失效不影响发布后前台可见性。

## 七、风险与规避

### 风险 1：前后端契约升级导致旧页面异常

规避：

- 新字段使用向后兼容方式新增
- 前端先容错处理再切换新展示

### 风险 2：Agent 状态流转与现有发布逻辑冲突

规避：

- 保留现有发布服务，新增人工确认层，而不是重写发布服务

### 风险 3：后台新增页面但权限未收紧

规避：

- 所有审核相关接口继续挂在管理员域下
- 路由守卫与后端权限同时校验

## 八、阶段产出物

本阶段完成后，至少应新增或完成：

- 优化后的 `/knowledge` 页面
- 扩展后的问答响应结构
- Agent 草稿审核流程
- 后台草稿审核页
- 对应测试与联调记录