# Resume Reference

Use only when the task is about the online resume at `/resume`.

## Canonical files

- Resume HTML: `frontend/public/resume/index.html`
- Resume/images assets: `frontend/public/images/`
- Nginx routing template: `frontend/nginx/default.conf.template`
- Checked-in nginx config: `frontend/nginx/default.conf`
- Docker mounts: `docker-compose.yml`

## Delivery model

`/resume` is not a Vue Router page.
It is served as static content by nginx and Docker bind mounts.

Implications:

- Content/style updates usually edit `frontend/public/resume/index.html`.
- Asset updates usually edit files under `frontend/public/images/`.
- If only mounted HTML/assets change, browser refresh may be enough.
- If nginx routes or Docker mounts change, recreate/rebuild the frontend container.

## Common pitfalls

- Adding a Vue route for `/resume`.
- Editing a Vue view when nginx/static HTML is the real source.
- Forgetting image paths under `/images/...`.
- Editing `default.conf` but not `default.conf.template` for Docker builds.

## Validation

- Check `/resume` redirects or resolves correctly.
- Check static images load.
- If running through Docker, ensure the container has picked up route/mount changes.