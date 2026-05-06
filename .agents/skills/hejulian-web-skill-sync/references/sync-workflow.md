# 结构同步流程

本文件用于把“自动化同步当前结构”和“智能按需加载”落成可重复执行的流程。

## 第一步：采集结构快照

运行：

```powershell
python E:\coding\hejulian-web\.agents\skills\hejulian-web-skill-sync\scripts\collect_structure_snapshot.py
```

如需保存到文件：

```powershell
python E:\coding\hejulian-web\.agents\skills\hejulian-web-skill-sync\scripts\collect_structure_snapshot.py --output E:\coding\hejulian-web\.agents\skills\hejulian-web-skill-sync\current-structure.md
```

脚本会收集：

- 仓库根目录关键条目
- `frontend/src` 的目录与关键文件
- `frontend/src/views` 的页面与管理端页面
- `backend` 主包结构
- `controller`、`controller/admin`、`service`、`agent`、`rag`、Mapper XML
- `deploy/`、`llm-bridge/`、`todo/`

## 第二步：只加载受影响的目标文档

根据快照差异，挑选最小文档集合：

- 前端差异：原 skill 的 `references/frontend.md`
- 后端差异：原 skill 的 `references/backend.md`
- Agent 差异：原 skill 的 `references/agent.md`
- 部署差异：原 skill 的 `references/deployment.md`
- `/knowledge` 差异：原 skill 的 `references/rag.md`
- 仅顶层导航差异：原 skill 的 `SKILL.md`

不要为了“确认一下”而把所有 references 都加载进上下文。

## 第三步：做结构型更新，而不是百科式扩写

优先更新：

- 新增或删除的真实目录
- 新增或变更的关键页面、路由、控制器、服务入口
- 会影响 AI 判断“应该读哪份 reference”的分流信息

避免更新：

- 只是想把每个文件都罗列一遍
- 只是补实现细节，但不影响结构理解
- 把多个领域内容搬运到主 `SKILL.md`

## 第四步：回查分流是否仍然清楚

修改后，确认：

- 主 `SKILL.md` 仍然能把任务准确分到对应 `references/*.md`
- 每份引用文档仍然聚焦单一领域
- 没有出现“要理解前端却必须先读 3 层引用”的深链

## 推荐维护口令

当用户想快速修这个 skill 时，可直接使用类似请求：

- “用 `hejulian-web-skill-sync` 同步 `hejulian-web-editing` 到当前代码结构”
- “检查 `hejulian-web-editing` 的结构说明，只修正失真和缺失部分”
- “保持按需加载不变，更新 `hejulian-web-editing` 的前端和 Agent 结构映射”
