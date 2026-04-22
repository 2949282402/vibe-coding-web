#!/bin/sh
set -eu

LOG_ROOT="${BLOG_LOG_ROOT:-/logs}"
LOG_DATE="$(date +%F)"
LOG_DIR="${LOG_ROOT}/${LOG_DATE}"
LOG_FILE="${LOG_DIR}/frontend.txt"

mkdir -p "${LOG_DIR}"
touch "${LOG_FILE}"

export NGINX_LOG_FILE="${LOG_FILE}"
envsubst '${NGINX_LOG_FILE}' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf

exec nginx -g 'daemon off;'
