# Review Reference

Read this file only for review tasks in `hejulian-web`.

## Review strategy

Do not load the whole repo by default.
Read only the files touched by the diff, then pull in the matching references:

- frontend changes: read `references/frontend.md`
- backend changes: read `references/backend.md`
- RAG or `/knowledge` changes: read `references/rag.md`
- Docker, nginx, static delivery, or `/resume` changes: read `references/deployment.md` and possibly `references/resume.md`

## Priority review themes

### Frontend

- route guard regressions
- public/admin layout regressions
- locale regressions
- data contract mismatch with backend

### Backend

- controller/service/mapper mismatch
- schema changes applied in only one place
- auth regressions
- upload path or static exposure regressions

### RAG

- source ownership regressions
- citation jump regressions
- history restore regressions
- SSE payload mismatch
- cache invalidation omissions

### Deployment and resume

- nginx route regressions
- Docker mount regressions
- static resume delivery regressions
- frontend SPA and static route conflicts

## Review checklist

Check what applies:

- backend payload still matches frontend consumption
- `/knowledge` still preserves source and citation behavior
- history restore still returns enough source data
- auth and admin checks still align across frontend and backend
- static `/resume` delivery still uses nginx and mounted files correctly

## Reporting

When reviewing:

- lead with findings
- prioritize bugs, regressions, and missing tests
- keep summaries brief
- include file references when possible
