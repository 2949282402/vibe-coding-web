# 第一阶段代码改造地图

## 一、前端改造地图

## 1. 问答主页面

### 文件

- `frontend/src/views/KnowledgeView.vue`

### 需要改的内容

- 重构模式选择区，移除前台 Agent 入口。
- 增加首页空态、示例问题、能力说明。
- 增加高级设置区，承载 Ask 与搜索模式切换。
- 增加回答元信息展示区。
- 调整低可信度、Web Search、无引用回答的提示文案。

### 风险

- 该文件可能已承载较多状态与 SSE 逻辑，改动时要避免把展示重构和流式逻辑混在一起。

## 2. API 适配层

### 文件

- `frontend/src/api/blog.js`
- `frontend/src/api/admin.js`

### 需要改的内容

- 适配 `AskResponse` 新字段。
- 增加草稿审核相关后台接口。
- 增加知识库运营统计接口。

## 3. 路由与后台页面

### 文件

- `frontend/src/router/index.js`
- `frontend/src/views/admin/AgentOpsView.vue`
- `frontend/src/views/admin/AgentToolCallsView.vue`
- 新增 `frontend/src/views/admin/AgentDraftReviewView.vue`
- 新增 `frontend/src/views/admin/KnowledgeOpsView.vue`

### 需要改的内容

- 新增后台路由。
- 调整 Agent 页入口关系。
- 将草稿审核纳入后台操作路径。

## 二、后端改造地图

## 1. RAG 入口与返回结构

### 文件

- `backend/src/main/java/com/hejulian/blog/controller/RagController.java`
- `backend/src/main/java/com/hejulian/blog/dto/RagDtos.java`

### 需要改的内容

- 扩展问答返回字段。
- 保证普通接口和流式接口最终结果结构一致。

## 2. RAG 业务逻辑

### 文件

- `backend/src/main/java/com/hejulian/blog/rag/application/RagApplicationService.java`
- `backend/src/main/java/com/hejulian/blog/rag/application/KnowledgeRetrievalService.java`
- `backend/src/main/java/com/hejulian/blog/rag/application/CitationGuardService.java`

### 需要改的内容

- 计算 `sourceType`、`retrievalHitCount`、`usedWebSearch`、`latencyMs`、`confidenceLevel`。
- 明确 Ask 模式分支。
- 统一普通问答与流式问答收口逻辑。

## 3. Agent 状态与审核链路

### 文件

- `backend/src/main/java/com/hejulian/blog/agent/application/AgentTaskApplicationService.java`
- `backend/src/main/java/com/hejulian/blog/agent/application/AgentOrchestratorService.java`
- `backend/src/main/java/com/hejulian/blog/service/AdminBlogService.java`
- `backend/src/main/java/com/hejulian/blog/agent/controller/` 下相关 controller
- `backend/src/main/java/com/hejulian/blog/agent/dto/` 下相关 DTO
- `backend/src/main/java/com/hejulian/blog/agent/entity/` 下相关 entity
- `backend/src/main/java/com/hejulian/blog/agent/mapper/` 下相关 mapper
- `backend/src/main/resources/mapper/` 下相关 XML

### 需要改的内容

- 新增草稿就绪状态。
- 调整执行完成后的保存逻辑。
- 新增审核发布、驳回接口。
- 保证取消、失败、驳回状态不会误入发布链。

## 三、数据库改造地图

## 1. 初始化脚本

### 文件

- `sql/blog_mysql_init.sql`

### 需要改的内容

- 新增 `rag_query_event` 表。
- 为 Agent 草稿审核补必要字段或新增审核表。
- 若项目已有 schema initializer，同步补兼容升级逻辑。

## 2. MyBatis 持久层

### 文件

- `backend/src/main/resources/mapper/*.xml`
- 对应 `entity` / `mapper` / `service`

### 需要改的内容

- 新增埋点读写 SQL。
- 新增草稿审核查询与状态更新 SQL。

## 四、缓存与部署改造地图

## 1. 缓存

### 关注文件

- `backend/src/main/java/com/hejulian/blog/common/CacheNames.java`
- 相关 service 中的缓存注解或手动失效逻辑

### 需要检查

- Agent 草稿发布后首页、文章列表、详情缓存失效。
- 问答反馈和统计是否需要单独缓存策略。

## 2. nginx 与 SSE

### 关注文件

- `frontend/nginx/default.conf.template`

### 需要检查

- 若后台新增页面代理或 SSE 路径调整，确认未破坏现有 `proxy_buffering off` 配置。

## 五、推荐实施顺序

1. 后端先扩展 `AskResponse`，前端做兼容接入。
2. 前端重构 `/knowledge` 页面结构。
3. 后端切 Agent 为草稿流。
4. 前端补后台草稿审核页。
5. 后端补问答埋点与统计接口。
6. 前端补知识库运营后台。

## 六、改造原则

- 先兼容，再替换，避免一次性推翻现有链路。
- 先改产品结构，再补运营数据，避免做完数据仍然服务于错误入口。
- 先把 Agent 从“自动生产并发布”改成“可审阅生产”，这是风险最低化的关键。