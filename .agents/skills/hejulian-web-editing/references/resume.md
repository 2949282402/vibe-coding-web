# Resume Reference

Read this file only when the task is about the online resume at `/resume`.

## Canonical files

- live resume HTML: `frontend/public/resume/index.html`
- live resume assets: `frontend/public/images/`
- nginx routing: `frontend/nginx/default.conf.template`
- Docker mount wiring: `docker-compose.yml`

## Delivery model

This resume is not part of the Vue Router app.

It is served as a static page through nginx and Docker bind mounts.
That means:

- editing `frontend/public/resume/index.html` is the normal path
- editing a Vue page is usually the wrong path
- if only the HTML content changes, a browser refresh should usually be enough
- if nginx or bind mounts change, the frontend container may need to be recreated once

## How to approach resume tasks

### Content or style update

Usually edit:

- `frontend/public/resume/index.html`

### Asset update

Usually edit or replace:

- files in `frontend/public/images/`

### Route or visibility issue

Inspect:

- `frontend/nginx/default.conf.template`
- `frontend/nginx/default.conf`
- `docker-compose.yml`

## Common pitfalls

- adding a Vue route for `/resume` when the live route is static
- changing the HTML but forgetting the image path it relies on
- changing nginx or mounts and expecting the running container to pick it up automatically
- debugging the Vue app when the actual problem is nginx routing or Docker bind mounts

## Validation

Check what applies:

- `http://127.0.0.1:180/resume` resolves to the static page
- assets under `/images/...` load correctly
- content changes are visible after refresh
- if not visible, verify mount path and container recreation status
