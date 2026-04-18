# HeJulian Blog

一个前后端分离的个人博客系统，采用当前常见且稳定的组合：

- 前端：`Vue 3 + Vite + Pinia + Vue Router + Element Plus`
- 后端：`Spring Boot 3 + Spring Security + JWT + MyBatis`
- 基础设施：`MySQL 8 + Redis 7 + Docker Compose`

项目默认按最常见的工程化结构组织，开发、测试、部署三条路径保持一致。

## 目录结构

```text
hejulian-web
├─ frontend/                # Vue 3 前端工程
│  ├─ src/
│  ├─ nginx/                # 前端容器内 Nginx 配置
│  └─ Dockerfile
├─ backend/                 # Spring Boot 后端工程
│  ├─ src/main/java/
│  ├─ src/main/resources/
│  ├─ src/test/
│  └─ Dockerfile
├─ sql/                     # MySQL 初始化脚本
├─ docs/                    # 项目文档
├─ deploy/                  # 兼容传统部署的模板文件
├─ docker-compose.yml       # 标准启动入口
└─ .env.example             # Compose 环境变量示例
```

## 功能范围

- 博客前台：首页、分类、归档、文章详情、评论提交
- 后台管理：登录、仪表盘、文章管理、分类管理、标签管理、评论审核
- 平台能力：JWT 认证、统一响应、全局异常处理、Redis 缓存、演示数据初始化

## 本地开发

### 方式一：Docker Compose

推荐直接使用 Compose，本地环境最接近服务器：

```powershell
Copy-Item .env.example .env
docker compose up -d --build
```

启动后默认访问地址：

- 前端：`http://localhost`
- 后端：`http://localhost:8080`
- 健康检查：`http://localhost/actuator/health`

### 方式二：分开启动

适合只改前端或只调后端时使用。

后端：

```powershell
cd backend
mvn spring-boot:run
```

前端：

```powershell
cd frontend
npm install
npm run dev
```

前端开发模式下会通过 Vite 代理把 `/api` 转发到 `http://localhost:8080`。

## 默认账号

- 用户名：`admin`
- 密码：`Admin123!`

首次启动后端时，`DataInitializer` 会自动写入演示数据。

## 数据库与缓存

- MySQL 使用 [sql/blog_mysql_init.sql](/E:/coding/hejulian-web/sql/blog_mysql_init.sql) 初始化表结构
- Redis 用于首页、分类、标签、文章列表等读多写少场景缓存
- 后端通过环境变量接入数据库和 Redis，便于本地、测试、服务器统一配置

## 测试与构建

后端：

```powershell
cd backend
mvn test
mvn -DskipTests package
```

前端：

```powershell
cd frontend
npm install
npm run build
```

## 部署说明

运维部署文档见 [docs/deployment-ops.md](/E:/coding/hejulian-web/docs/deployment-ops.md)。

接口总览见 [docs/api-overview.md](/E:/coding/hejulian-web/docs/api-overview.md)。
