# 简历页参考

只在处理线上简历页 `/resume` 时读取本文件。

## 权威文件

- 简历 HTML：`frontend/public/resume/index.html`
- 简历 / 公共图片资源：`frontend/public/images/`
- nginx 路由模板：`frontend/nginx/default.conf.template`
- 仓库内保留 nginx 配置：`frontend/nginx/default.conf`
- Docker 挂载定义：`docker-compose.yml`

## 投递模型

`/resume` 不是 Vue Router 页面。
它由 nginx 配合 Docker 挂载直接提供静态内容。

这意味着：

- 内容或样式更新通常直接修改 `frontend/public/resume/index.html`
- 资源文件更新通常修改 `frontend/public/images/`
- 如果只是挂载的 HTML / 资源变化，浏览器刷新通常就能看到
- 如果改了 nginx 路由或 Docker 挂载，则需要重建或重启前端容器

## 常见坑

- 给 `/resume` 额外加 Vue 路由
- 误改 Vue 页面，而真正的源头是静态 HTML
- 忘记资源路径应该走 `/images/...`
- Docker 场景下只改 `default.conf`，没改 `default.conf.template`

## 验证

- 检查 `/resume` 是否正确解析或重定向
- 检查静态图片是否正常加载
- 如果走 Docker，确认容器已经拾取新的路由或挂载变化
