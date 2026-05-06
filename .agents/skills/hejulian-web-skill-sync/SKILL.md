---
name: hejulian-web-skill-sync
description: 用于修缮和同步 E:\coding\hejulian-web\.agents\skills\hejulian-web-editing：自动采集当前仓库结构、只更新与真实结构不一致或缺失的说明、保持现有 references 按需加载分流，并优化该 skill 的结构导航与维护流程。适用于要求 AI 快速维护这个 skill、同步最新目录与入口、或增强其智能按需加载能力时。
---

# hejulian-web-editing 维护指南

只在维护 `E:\coding\hejulian-web\.agents\skills\hejulian-web-editing` 时使用这个 skill。
目标是让另一个 AI 能快速、稳定地把该 skill 同步到仓库现状，同时保留它现有的按需加载机制。

## 核心原则

1. 先同步事实，再改文档。
2. 保留原有 `SKILL.md + references/*.md` 的按需分流，不把多个领域合并成大文档。
3. 只修改与当前仓库结构不一致、明显缺失或会误导分流判断的部分。
4. 如果新增内容能放进现有引用文档，就不要新建无关文档。
5. 优先让结构导航更准，而不是把所有实现细节塞进 skill。

## 标准流程

1. 先读 `references/targets.md`，确认原 skill 各文件负责什么范围。
2. 运行 `scripts/collect_structure_snapshot.py`，生成当前仓库结构快照。
3. 按任务类型只读取需要更新的目标文档：
   - 顶层结构或主导航：读原 skill 的 `SKILL.md`
   - 前端结构：读原 skill 的 `references/frontend.md`
   - 后端结构：读原 skill 的 `references/backend.md`
   - `/knowledge`、RAG、Ask：读原 skill 的 `references/rag.md`
   - Agent：读原 skill 的 `references/agent.md`
   - 部署、Docker、nginx：读原 skill 的 `references/deployment.md`
   - `/resume`：读原 skill 的 `references/resume.md`
   - 评审规则：读原 skill 的 `references/review.md`
4. 对照快照，只修正文档中失真、缺失或过时的结构描述。
5. 检查修改后是否仍然保持“任务分类 -> 按需读对应 references”的分流路径清晰。

## 智能按需加载要求

- 主 `SKILL.md` 只保留入口导航、项目快照和跨层约束。
- 结构细节放到对应 `references/*.md`，不要把前端、后端、部署细节重新堆回主文档。
- 新增结构说明时，优先放到最贴近责任边界的引用文档里。
- 如果某个新模块横跨多个区域，只在主 `SKILL.md` 里做一句导航提示，详细内容仍拆到对应引用文档。
- 引用文档之间允许互相指引，但不要形成多层级深链；尽量让主 `SKILL.md` 直接告诉使用者该读哪一份。

## 什么时候补结构，什么时候不动

应修改：

- 真实目录、文件、路由、包结构、服务边界已经变化
- 原文缺少会影响任务分流判断的重要入口
- 原文把不存在的路径或职责写错了

不应修改：

- 只是措辞风格不同，但事实没变
- 只是想把文档写得更长、更全
- 会破坏现有按需加载边界的“合并式整理”

## 输出要求

- 所有维护文档默认写中文。
- 保持 UTF-8。
- 如果新增脚本或辅助文件，目的必须直接服务于结构同步或按需加载判断。
- 完成后说明：更新了哪些结构认知、保留了哪些分流机制、是否新增了自动化辅助。
