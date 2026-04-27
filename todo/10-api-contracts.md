# 第一阶段 API 契约草案

## 一、目标

定义第一阶段改造涉及的核心接口契约，确保前后端可以并行开发，并减少联调阶段反复返工。

## 二、RAG / Ask 响应扩展

## 1. 接口

- `POST /api/public/rag/ask`
- `POST /api/public/rag/ask/stream`

## 2. AskRequest

建议继续沿用现有结构：

```json
{
  "question": "如何部署这个博客？",
  "topK": 4,
  "sessionId": "uuid-optional",
  "searchMode": "LOCAL_ONLY",
  "answerMode": "RAG"
}
```

### 说明

- `answerMode`：`RAG | ASK`
- `searchMode`：`LOCAL_ONLY | WEB_ONLY | LOCAL_AND_WEB`
- `ASK` 模式下仍允许保留 `searchMode` 字段，但后端可忽略其本地检索含义

## 3. AskResponse

建议在现有结构上新增以下字段：

```json
{
  "sessionId": "uuid",
  "question": "如何部署这个博客？",
  "answer": "...",
  "mode": "retrieval",
  "sources": [],
  "history": [],
  "strictCitation": true,
  "searchMode": "LOCAL_ONLY",
  "sourceType": "local",
  "retrievalHitCount": 3,
  "usedWebSearch": false,
  "latencyMs": 1280,
  "confidenceLevel": "high",
  "knowledgeUpdatedAt": "2026-04-27T10:20:30Z"
}
```

### 字段定义

- `sourceType`：
  - `local`：主要依赖站内知识
  - `web`：主要依赖网络搜索
  - `mixed`：同时依赖站内和网络
  - `none`：未引用站内知识，通常用于 Ask
- `retrievalHitCount`：实际命中的本地来源数量
- `usedWebSearch`：是否调用 Web Search
- `latencyMs`：完整响应耗时，单位毫秒
- `confidenceLevel`：`high | medium | low`
- `knowledgeUpdatedAt`：本次回答主要依据内容的更新时间，可为空

### 兼容策略

- 以上字段全部新增，不删除旧字段
- 前端对新增字段做空值兼容
- 流式 `done` 事件中的最终 payload 与普通 `ask` 响应保持一致

## 4. SSE 事件结构

继续保持：

```json
{
  "type": "meta | delta | done | error",
  "data": {}
}
```

其中：

- `meta`：初始状态，可附带 `sessionId`
- `delta`：增量文本
- `done`：完整 `AskResponse`
- `error`：错误信息

## 三、Agent 草稿审核接口

## 1. 草稿列表

### 接口

- `GET /api/admin/agent/drafts`

### 查询参数建议

- `status`：可选，`DRAFT_READY | REVIEW_REJECTED | PUBLISHED`
- `keyword`：标题关键字
- `page`
- `size`

### 返回示例

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "items": [
      {
        "taskId": 101,
        "postId": 55,
        "title": "RAG 系统设计复盘",
        "status": "DRAFT_READY",
        "summary": "文章围绕检索、生成和引用链路展开。",
        "createdAt": "2026-04-27T10:00:00Z",
        "updatedAt": "2026-04-27T10:30:00Z",
        "reviewedBy": null,
        "reviewedAt": null,
        "rejectReason": null
      }
    ],
    "page": 1,
    "size": 10,
    "total": 1
  }
}
```

## 2. 草稿详情

### 接口

- `GET /api/admin/agent/drafts/{taskId}`

### 返回建议

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "taskId": 101,
    "status": "DRAFT_READY",
    "title": "RAG 系统设计复盘",
    "content": "...markdown or html...",
    "summary": "...",
    "sourceType": "mixed",
    "traceAvailable": true,
    "postId": 55,
    "createdAt": "2026-04-27T10:00:00Z"
  }
}
```

## 3. 审核通过并发布

### 接口

- `POST /api/admin/agent/drafts/{taskId}/approve`

### 请求体建议

```json
{
  "publish": true,
  "editorNote": "结构完整，可发布"
}
```

### 返回建议

```json
{
  "code": 200,
  "message": "Published",
  "data": {
    "taskId": 101,
    "status": "PUBLISHED",
    "postId": 55,
    "publishedAt": "2026-04-27T11:00:00Z"
  }
}
```

## 4. 驳回草稿

### 接口

- `POST /api/admin/agent/drafts/{taskId}/reject`

### 请求体建议

```json
{
  "reason": "引用不足，需要补充站内依据",
  "editorNote": "先保留草稿，不发布"
}
```

### 返回建议

```json
{
  "code": 200,
  "message": "Rejected",
  "data": {
    "taskId": 101,
    "status": "REVIEW_REJECTED",
    "rejectReason": "引用不足，需要补充站内依据"
  }
}
```

## 四、知识库运营接口

## 1. 总览接口

### 接口

- `GET /api/admin/knowledge/overview`

### 返回建议

```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "queryCount": 1234,
    "helpfulRate": 0.71,
    "avgLatencyMs": 1420,
    "webSearchRate": 0.28,
    "noHitRate": 0.16,
    "indexedPostCount": 86,
    "indexedChunkCount": 2640
  }
}
```

## 2. 问题统计接口

### 接口

- `GET /api/admin/knowledge/query-stats`

### 查询参数建议

- `dateFrom`
- `dateTo`
- `answerMode`
- `searchMode`

## 3. 无命中问题接口

### 接口

- `GET /api/admin/knowledge/unanswered-top`

### 返回字段建议

- `question`
- `count`
- `lastAskedAt`

## 4. 低帮助率问题接口

### 接口

- `GET /api/admin/knowledge/low-helpful-top`

### 返回字段建议

- `question`
- `helpfulRate`
- `count`
- `lastAskedAt`

## 五、错误码建议

建议复用现有 `ApiResponse`，但新增语义化错误场景：

- Agent 草稿不存在
- Agent 草稿状态不允许发布
- Agent 草稿状态不允许驳回
- 非管理员访问审核接口
- Web Search 请求与模型能力不匹配

## 六、联调原则

- 新增字段全部向后兼容
- 草稿审核接口只面向管理员
- 统计接口优先保证字段稳定，图表形式由前端自由适配