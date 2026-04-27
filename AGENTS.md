# Repository Guidelines

## Project Structure & Module Organization
- `frontend/`: Vue 3 + Vite app. Main code lives in `frontend/src/` with routes in `src/router`, API clients in `src/api`, stores in `src/stores`, layouts in `src/layouts`, and views in `src/views`.
- `backend/`: Spring Boot app. Java sources live in `backend/src/main/java/com/hejulian/blog`, MyBatis XML files in `backend/src/main/resources/mapper`, and tests in `backend/src/test/java`.
- `sql/` contains MySQL bootstrap scripts, `deploy/` and `docker-compose.yml` hold deployment wiring, `docs/` stores operational notes, and `frontend/public/resume/` serves the static `/resume` page.

## Build, Test, and Development Commands
- `cd frontend; npm run dev` - start the Vite dev server.
- `cd frontend; npm run build` - produce the production frontend bundle; run this after UI or route changes.
- `cd backend; mvn test` - run backend tests.
- `cd backend; mvn -DskipTests package` - build the backend jar.
- `docker compose up -d --build` - start the full local stack with MySQL, Redis, backend, and frontend.

## Coding Style & Naming Conventions
- Use UTF-8 for all edits. Keep Chinese and English user-facing copy aligned when both are present.
- Vue files, views, and layouts use PascalCase names such as `KnowledgeView.vue` and `AdminLayout.vue`; stores and API modules use lowercase names such as `auth.js` and `blog.js`.
- Follow existing formatting: 2-space indentation in frontend code, 4-space indentation in Java/XML, and descriptive Spring class names such as `AuthController` and `RagApplicationService`.
- No dedicated formatter or linter is wired in this repo; match the surrounding style and keep imports tidy.

## Testing Guidelines
- Backend tests use Spring Boot Test with JUnit under `backend/src/test/java`; follow the `*Tests.java` naming pattern.
- Add or update tests when changing controller, service, security, or persistence behavior.
- The frontend has no automated test suite yet, so at minimum run `npm run build` and exercise affected flows locally.

## Commit & Pull Request Guidelines
- Recent commits are short and direct, with brief feature- or fix-oriented subjects such as `add agent`. Keep the same style: concise, specific, and scoped to one change.
- PRs should describe the user-visible change, list affected areas (`frontend`, `backend`, `sql`, deployment), link related issues, and include screenshots for UI updates.
- Call out schema, cache, auth, or SSE impacts explicitly so reviewers can validate cross-layer changes.