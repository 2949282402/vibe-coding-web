# 后端参考

用于处理控制器、服务、鉴权、上传、公共/管理 API、MyBatis 持久层、表结构、缓存和运行时配置。

## 技术栈

- Spring Boot 3
- Java 21 容器构建
- MyBatis XML Mapper
- Spring Security + JWT
- Redis 驱动的 Spring Cache
- MySQL

## 包结构总览

基础包：`backend/src/main/java/com/hejulian/blog/`

- `common/`：统一响应、分页响应、缓存名等基础常量。
- `config/`：安全、跨域、Redis、Web 静态映射等配置。
- `controller/`：公共、认证、RAG 控制器。
- `controller/admin/`：后台仪表盘、文章、分类标签、评论、RAG 反馈、上传接口。
- `dto/`：REST DTO，例如 `AuthDtos`、`RagDtos`、`AdminDtos`。
- `entity/`：博客主域持久化实体。
- `mapper/`：博客主域与 RAG 会话等 Mapper 接口。
- `repository/`：仓库抽象或聚合访问入口。
- `security/`：JWT、用户详情与安全相关组件。
- `service/`：主域服务、初始化、上传、鉴权、Python 桥接。
- `service/impl/`：服务实现类。
- `rag/`：RAG / Ask 的 application、config、domain、infrastructure 分层。
- `agent/`：Agent 的 application、controller、domain、dto、entity、mapper、infrastructure 分层。

## 关键控制器

- `PublicBlogController.java`：首页、文章列表/详情、评论、公共检索。
- `AuthController.java`：登录、注册、个人资料、Qwen 配置。
- `RagController.java`：`/api/public/rag/**`，处理普通问答与 SSE。
- `controller/admin/` 下的后台控制器：文章、分类、评论、反馈、上传、仪表盘。
- `agent/controller/AgentTaskController.java`：`/api/agent/tasks/**`，管理员 Agent 任务与流式接口。
- `agent/controller/AgentMemoryController.java`、`AgentToolController.java`：Agent memory/tool 接口。
- `agent/controller/admin/AdminAgentController.java`：Agent 后台运维接口。

## 服务职责

- `AuthService.java`：账号、个人资料、Qwen 运行时配置。
- `AdminBlogService.java`：后台文章 CRUD，也是 Agent 发布文章的落点。
- `PublicBlogService.java`：前台站点聚合、详情、搜索。
- `RagApplicationService.java`：RAG、Ask、SSE、历史、重放、反馈。
- `RagIndexingApplicationService.java`：知识索引与切片入库流程。
- `AgentOrchestratorService.java`：Agent planner / researcher / writer / reviewer / publisher 编排。
- `AgentTaskApplicationService.java`：Agent 任务创建、详情、列表、取消、重试、SSE 快照。
- `AgentAdminService.java`、`AgentToolService.java`、`AgentTraceService.java`、`AgentMemoryService.java`：Agent 运维、工具调用、执行轨迹、记忆管理。

## 持久层检查清单

改动任何存储结构时，至少检查：

- 实体类
- Mapper 接口
- `backend/src/main/resources/mapper/` 下的 Mapper XML
- SQL 初始化脚本：`sql/blog_mysql_init.sql`
- Schema Initializer：`UserSchemaInitializer.java`、`RagSchemaInitializer.java`、`AgentSchemaInitializer.java`
- 服务层缓存失效逻辑

## 安全预期

- `/api/agent/**` 需要管理员角色。
- `/api/public/rag/**` 以 `SecurityConfig.java` 的当前配置为准，部分接口要求登录。
- 评论、上传与后台写接口仍受鉴权与管理员权限保护。

## 缓存敏感区域

- 缓存名常量：`backend/src/main/java/com/hejulian/blog/common/CacheNames.java`
- Redis 配置：`backend/src/main/java/com/hejulian/blog/config/RedisConfig.java`
- 公开文章写操作需要驱逐首页、文章列表、详情相关缓存。
- RAG / Ask / Agent 的历史写入需要驱逐会话列表与历史缓存。
- Qwen 配置查询保持读多写少，能力探测通常放在保存/更新路径。

## 验证

- 本地有 Maven 时，按需执行 `mvn test` 或 `mvn -DskipTests package`。
- 本地没有 Maven 时，从仓库根目录执行 `docker compose build backend`。
