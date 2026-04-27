# Todo 文档索引

本目录用于沉淀当前 `hejulian-web` 项目的完整优化方案，覆盖产品定位、功能增删改建议、技术落地方案、开发任务拆解、代码改造地图、API 契约、数据库变更、测试计划、风险台账与实际代码实施顺序。

## 文档列表

- `01-current-state-review.md`：当前项目现状、问题拆解、核心矛盾。
- `02-product-optimization-plan.md`：完整产品优化建议，包含新增、移除、降级与体验优化。
- `03-technical-solution.md`：围绕知识问答、Agent、后台运营的技术方案。
- `04-roadmap-and-priority.md`：按优先级拆分的迭代路线与实施顺序。
- `05-data-and-metrics.md`：建议新增的数据埋点、运营指标与效果评估方法。
- `06-development-task-breakdown.md`：按前端、后端、数据库、联调拆解的开发任务清单。
- `07-phase1-implementation-plan.md`：第一阶段 P0 改造的详细实施方案。
- `08-phase1-engineering-work-items.md`：第一阶段工单化任务清单。
- `09-code-change-map.md`：第一阶段涉及文件与改造点的代码地图。
- `10-api-contracts.md`：第一阶段接口契约草案。
- `11-database-changes.md`：第一阶段数据库变更方案。
- `12-test-plan.md`：第一阶段测试计划。
- `13-risk-register.md`：第一阶段风险台账、回滚策略与上线注意事项。
- `14-phase1-code-implementation-sequence.md`：第一阶段实际代码实施顺序。

## 总体判断

当前项目已经具备博客站点、RAG/Ask 问答、管理员 Agent 写作三类能力，但三者的产品边界不够清晰，导致用户路径、管理路径和系统复杂度混在一起。后续优化的重点不应继续盲目加模型能力，而应先完成：

1. 场景拆分：区分普通用户问答与管理员生产工具。
2. 提升信任：增强 RAG 回答的来源解释和可信度表达。
3. 收紧发布：Agent 默认改为草稿流，而不是自动直发。
4. 建立运营闭环：补足知识库质量、问答效果、Agent 产出质量的后台指标。

## 推荐执行顺序

1. 先读 `01-current-state-review.md`
2. 再看 `02-product-optimization-plan.md`
3. 按 `03-technical-solution.md` 明确系统改造边界
4. 按 `06-development-task-breakdown.md` 拆分研发任务
5. 用 `07-phase1-implementation-plan.md` 推进第一阶段落地
6. 用 `08-phase1-engineering-work-items.md` 分配工单
7. 用 `09-code-change-map.md` 定位具体改造文件
8. 用 `10-api-contracts.md` 做前后端并行联调
9. 用 `11-database-changes.md` 规划表结构与持久层改造
10. 用 `12-test-plan.md` 制定联调与验收路径
11. 用 `13-risk-register.md` 做上线前风险控制与回滚准备
12. 用 `14-phase1-code-implementation-sequence.md` 指导实际代码改造顺序
13. 最后依据 `04-roadmap-and-priority.md` 排期实施