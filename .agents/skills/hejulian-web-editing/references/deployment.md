# 部署参考

用于处理 Docker、nginx、运行时配置、日志、上传暴露、SSE 缓冲和静态资源投递。

## 根目录部署文件

- Compose：`docker-compose.yml`
- 后端镜像：`backend/Dockerfile`、`backend/docker-entrypoint.sh`
- 前端镜像：`frontend/Dockerfile`、`frontend/docker-entrypoint.sh`
- 前端 nginx 模板：`frontend/nginx/default.conf.template`
- 仓库内保留的 nginx 配置：`frontend/nginx/default.conf`
- 外部 nginx 示例：`deploy/nginx/hejulian-blog.conf`
- deploy 附属资源：`deploy/env/`、`deploy/scripts/`、`deploy/systemd/`
- 运行时配置：`backend/src/main/resources/application.yml`
- SQL 初始化：`sql/blog_mysql_init.sql`
- 日志目录：`logs/YYYY-MM-DD/`

## Docker 服务

常见服务包括：

- `mysql`
- `redis`
- `qdrant`
- `backend`
- `frontend`
- 可选 profile：`llm-bridge`

## 前端投递方式

前端容器会：

- 先执行 `npm run build` 构建 Vue 静态产物
- 用 nginx 提供 `dist/`
- 在容器启动时根据 `frontend/nginx/default.conf.template` 生成 `/etc/nginx/conf.d/default.conf`
- 通过挂载方式提供 `frontend/public/resume/` 下的 `/resume`
- 通过挂载方式提供 `frontend/public/images/` 下的公共图片

重点：对 Docker 生效的是真正进入镜像的 `default.conf.template`；只改 `default.conf` 不会更新容器启动配置。

## 后端投递方式

后端容器会：

- 在 Docker 内执行 Maven 构建
- 提供 JSON API、RAG / Ask SSE、Agent SSE、上传能力和健康检查
- 通过挂载的 `./logs` 输出按日期分目录的日志

## `llm-bridge`

- 目录：`llm-bridge/server.py`、`Dockerfile`、`docker-entrypoint.sh`
- 在 `docker-compose.yml` 中使用可选 profile `llm-bridge`
- 主要用于对接可选 Python / Ollama 桥接能力，不是默认必启服务

## 必须关闭缓冲的 SSE 路径

- RAG / Ask：`/api/public/rag/ask/stream`
- Agent 任务：`/api/agent/tasks/{taskId}/stream`

nginx 需要：

- `proxy_buffering off;`
- `proxy_cache off;`
- 较长的 `proxy_read_timeout` 与 `proxy_send_timeout`
- `add_header X-Accel-Buffering no;`

## 运行时模型配置

- 系统 / 环境默认值在 `docker-compose.yml` 与 `application.yml`
- 用户自己的 Qwen API Key、模型与联网能力保存在账号配置接口里
- `DASHSCOPE_API_KEY` 可以提供系统级兜底，但 RAG / Ask / Agent 聊天优先读取用户运行时配置

## 日志

日志按日期写入 `logs/YYYY-MM-DD/`。
常见文件包括 backend / frontend / mysql / redis / qdrant 日志，具体取决于启用的服务。

## 验证命令

- 仅前端：在 `frontend/` 下执行 `npm run build`
- 仅后端：在仓库根目录执行 `docker compose build backend`
- 全量重建：`docker compose up -d --build`
- 使用已构建镜像重启主要服务：`docker compose up -d backend frontend`

## 常见坑

- 改了生成后的 nginx 配置，却没改模板
- 改了 nginx / 模板，却没重建或重建后没重启容器
- 误以为 `/resume` 是 Vue 路由
- 只盯着 Java 调 SSE，却忽略 nginx 缓冲
- 忘了 Docker build context，导致前端模板没有真正进入镜像
