# API Overview

## Authentication

- `POST /api/auth/login`

Request:

```json
{
  "username": "admin",
  "password": "Admin123!"
}
```

## Public APIs

- `GET /api/public/site`
  Returns homepage data, categories, tags, and statistics.
- `GET /api/public/posts`
  Returns the published post list with keyword/category/tag filters.
- `GET /api/public/posts/{slug}`
  Returns a post detail and approved comments.
- `POST /api/public/comments`
  Submits a comment for moderation.
- `POST /api/public/rag/ask`
  Runs retrieval-augmented question answering over published blog content.
- `GET /api/public/rag/history`
  Returns the current RAG conversation history by `sessionId`.
- `POST /api/public/rag/feedback`
  Stores helpful/not-helpful feedback for an assistant answer.
- `POST /api/public/rag/ask/stream`
  Streams RAG answers over Server-Sent Events for slow local models.

Comment request example:

```json
{
  "postId": 1,
  "nickname": "Visitor",
  "email": "visitor@example.com",
  "content": "This article is helpful."
}
```

RAG request example:

```json
{
  "sessionId": "a5f5d2e1-...",
  "question": "How is this blog deployed?",
  "topK": 4
}
```

RAG response fields:

- `answer`: final answer text
- `mode`: `retrieval` or `llm`
- `llmEnabled`: whether external generation is configured
- `sessionId`: conversation session identifier
- `indexedPosts`: number of published posts indexed
- `indexedChunks`: number of chunks in the knowledge base
- `sources`: matched source excerpts, slugs, and citation indexes
- `history`: chat history in the current session
- `strictCitation`: whether the answer is enforced to include citations
- `followUpQuestions`: suggested follow-up prompts
- `feedbackHelpful`, `feedbackNote`, `feedbackAt`: stored answer feedback state

Generation notes:

- Retrieval-only mode works without any LLM service
- The standard RAG pipeline is `embedding recall -> rerank -> generation`
- Vector recall is backed by `Qdrant`
- Embedding uses `text-embedding-v4`
- Rerank uses `qwen3-rerank`
- Generation uses `qwen-max`
- The streaming endpoint emits `meta`, `delta`, `done`, and `error` events

## Admin APIs

All `/api/admin/**` endpoints require:

```text
Authorization: Bearer <token>
```

- `GET /api/admin/dashboard`
  Returns content metrics plus RAG feedback summary and recent feedback entries.
- `GET /api/admin/rag-feedback`
  Returns paged RAG feedback entries with `helpful`, `keyword`, `feedbackDateFrom`, and `feedbackDateTo` filters.
- `GET /api/admin/rag-feedback/export`
  Exports filtered RAG feedback as CSV.
- `GET /api/admin/posts`
- `GET /api/admin/posts/{id}`
- `POST /api/admin/posts`
- `PUT /api/admin/posts/{id}`
- `DELETE /api/admin/posts/{id}`
- `GET /api/admin/categories`
- `POST /api/admin/categories`
- `PUT /api/admin/categories/{id}`
- `DELETE /api/admin/categories/{id}`
- `GET /api/admin/tags`
- `POST /api/admin/tags`
- `PUT /api/admin/tags/{id}`
- `DELETE /api/admin/tags/{id}`
- `GET /api/admin/comments`
- `PUT /api/admin/comments/{id}/status`
