# Review Reference

Use for code review or final pass on changes in `hejulian-web`.

## Review strategy

1. Identify changed files or affected feature area.
2. Read only those files plus the matching reference.
3. Trace contracts across frontend, backend, persistence, cache, and deployment when the change crosses layers.
4. Report findings first, ordered by severity, with file/line references.

## Priority findings by area

### Frontend

- Route guard regressions.
- Admin-only UI exposed to normal users.
- Locale/copy regressions or corrupted UTF-8 strings.
- API payload mismatch with backend DTOs.
- `/knowledge` state regressions across RAG, Ask, and Agent modes.
- Source/citation rendering or history restore breakage.

### Backend

- Controller/service/DTO/mapper mismatch.
- MyBatis XML column/value count mismatch.
- Schema changes missing from SQL bootstrap or initializer.
- Auth/security regressions.
- Cache invalidation omissions after writes.
- Runtime config not applied through `RagRuntimeContextHolder`.

### RAG/Ask

- Ask accidentally using retrieval or requiring citations.
- RAG losing source ownership or citation mappings.
- SSE payload mismatch between backend `StreamEvent` and frontend parser.
- Replay/feedback/history writes not preserving sources/variants.

### Agent

- Task creation/retry running synchronously and hiding progress.
- Cancellation not honored before publish/final status.
- Tool calls not recorded or not visible in trace.
- Final article containing plan/debug/tool text.
- Admin-only backend route not enforced.
- Published post caches not evicted.

### Deployment

- Nginx stream endpoints buffered.
- Template/config mismatch for frontend nginx.
- Docker compose env/mount mismatch.
- `/resume` static route broken by Vue SPA routing.

## Residual risk notes

When no findings are found, mention any unvalidated risk such as no runtime smoke test, no Docker rebuild, or no live SSE check.