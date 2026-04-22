# Backend Reference

Read this file only for backend-facing tasks.

## Backend stack

- Spring Boot 3
- MyBatis
- Spring Security
- JWT auth
- Redis-backed Spring Cache
- MySQL persistence

## Backend structure

### Common and config

- common responses and helpers: `backend/src/main/java/com/hejulian/blog/common/`
- config: `backend/src/main/java/com/hejulian/blog/config/`
- global exception handling: `backend/src/main/java/com/hejulian/blog/exception/`
- security: `backend/src/main/java/com/hejulian/blog/security/`

### Controllers

- public/auth controllers: `backend/src/main/java/com/hejulian/blog/controller/`
- admin controllers: `backend/src/main/java/com/hejulian/blog/controller/admin/`

### Services

- service layer: `backend/src/main/java/com/hejulian/blog/service/`
- main services include `PublicBlogService.java`, `AuthService.java`, `AdminBlogService.java`, `UploadStorageService.java`

### Persistence

- entities: `backend/src/main/java/com/hejulian/blog/entity/`
- mapper interfaces: `backend/src/main/java/com/hejulian/blog/mapper/`
- MyBatis XML: `backend/src/main/resources/mapper/`
- base SQL bootstrap: `sql/blog_mysql_init.sql`

### Schema compatibility helpers

- user compatibility: `backend/src/main/java/com/hejulian/blog/service/UserSchemaInitializer.java`
- bootstrap/demo data: `backend/src/main/java/com/hejulian/blog/service/DataInitializer.java`

## Controller map

### Public and auth

- `PublicBlogController.java`: homepage, public post list, post detail, comment submission path
- `AuthController.java`: login, register, current profile, Qwen config
- `RagController.java`: RAG APIs and SSE stream

### Admin

- `AdminDashboardController.java`: admin overview stats
- `AdminPostController.java`: post CRUD and editor-facing operations
- `AdminTaxonomyController.java`: category and tag CRUD
- `AdminCommentController.java`: comment moderation
- `AdminRagFeedbackController.java`: feedback management and export
- `AdminUploadController.java`: image uploads used by the editor

## Service responsibilities

- `PublicBlogService.java`: public homepage aggregation, public article data, public-facing browsing logic
- `AuthService.java`: auth, profile resolution, Qwen config persistence, runtime option resolution
- `AdminBlogService.java`: admin CRUD for posts and taxonomies, dashboard-adjacent content operations
- `UploadStorageService.java`: uploaded file persistence and path generation
- `PythonBridgeClient.java`: optional local bridge integration for model workflows

## Security and auth

- security config: `backend/src/main/java/com/hejulian/blog/config/SecurityConfig.java`
- JWT provider and filter: `backend/src/main/java/com/hejulian/blog/security/`
- authenticated principal model: `AuthenticatedUser.java`

Important assumptions:

- comments and RAG require authenticated users
- admin routes require admin authority
- frontend route guards and backend security must stay aligned

## Persistence and schema guidance

If a change affects stored data, inspect:

- entity
- mapper interface
- mapper XML
- service or controller caller
- `sql/blog_mysql_init.sql`
- `UserSchemaInitializer.java` when user-table compatibility matters

Do not patch only one layer if the data shape crosses controller, service, mapper, and SQL boundaries.

## Runtime config

Main runtime config lives in:

- `backend/src/main/resources/application.yml`

This file contains:

- datasource config
- Redis config
- app info
- RAG settings
- JWT settings

If the task touches runtime behavior, env-based defaults, or model settings, read `application.yml` early.

## Caching

Main cache names live in:

- `backend/src/main/java/com/hejulian/blog/common/CacheNames.java`

Redis cache wiring lives in:

- `backend/src/main/java/com/hejulian/blog/config/RedisConfig.java`

Watch for cache invalidation when changing:

- public site aggregation
- Qwen config
- RAG history
- RAG sessions
- feedback or replay flows

## Uploads and static exposure

- upload handling: `AdminUploadController.java`, `UploadStorageService.java`
- static exposure for uploads: `backend/src/main/java/com/hejulian/blog/config/WebConfig.java`

If the task touches publicly accessible files, also read `references/deployment.md`.

## Validation

Preferred validation:

- targeted backend tests when available
- Maven build or package commands when available

If the task touches chat or retrieval behavior, also read `references/rag.md`.
