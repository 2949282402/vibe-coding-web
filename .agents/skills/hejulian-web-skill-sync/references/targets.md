# 目标文件分工

本文件用于帮助维护者快速判断：应该更新原 skill 的哪一份文档。

## 原 skill 文件职责

- `E:\coding\hejulian-web\.agents\skills\hejulian-web-editing\SKILL.md`
  - 负责项目总览、任务分类、分流入口、跨层约束、建议验证
- `references/frontend.md`
  - 负责前端目录、布局、路由、前端 API、Store、`/knowledge` 前端入口
- `references/backend.md`
  - 负责后端包结构、控制器、服务、持久层、缓存、安全
- `references/rag.md`
  - 负责 `/knowledge` 的 RAG / Ask / SSE / 历史 / 反馈链路
- `references/agent.md`
  - 负责 Agent 工作流、后端 agent 包、前端管理页、工具与轨迹
- `references/deployment.md`
  - 负责 Docker、nginx、环境变量、日志、`llm-bridge`
- `references/resume.md`
  - 负责静态 `/resume` 页面与其 nginx / Docker 投递方式
- `references/review.md`
  - 负责代码评审视角下的高风险点

## 更新规则

- 顶层目录、全局分流入口变化：优先改原 `SKILL.md`
- 某个领域新增了关键入口或页面：只改对应 `references/*.md`
- 只有在“新模块会影响任务如何分流”时，才同时补一条到原 `SKILL.md`
- 除非现有拆分已经失效，否则不要新增新的 references 文件

## 结构同步的最小闭环

更新某一份文档前，至少核对这些事实：

- 真实目录或文件是否存在
- 入口文件名是否正确
- 路由或接口路径是否仍然一致
- 该说明是否应该放在当前文档，而不是别的 references 文档
