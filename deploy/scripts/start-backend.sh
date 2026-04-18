#!/usr/bin/env bash
set -euo pipefail

APP_HOME="/opt/hejulian-blog/app/backend"
JAR_NAME="blog-backend-1.0.0.jar"

if [[ ! -f "${APP_HOME}/${JAR_NAME}" ]]; then
  echo "backend jar not found: ${APP_HOME}/${JAR_NAME}" >&2
  exit 1
fi

if [[ -f "${APP_HOME}/blog-backend.env" ]]; then
  set -a
  . "${APP_HOME}/blog-backend.env"
  set +a
fi

exec java -Xms256m -Xmx512m -jar "${APP_HOME}/${JAR_NAME}"
