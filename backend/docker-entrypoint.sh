#!/bin/sh
set -eu

LOG_ROOT="${BLOG_LOG_ROOT:-/logs}"
LOG_DATE="$(date +%F)"
LOG_DIR="${LOG_ROOT}/${LOG_DATE}"
LOG_FILE="${LOG_DIR}/backend.txt"

mkdir -p "${LOG_DIR}"
touch "${LOG_FILE}"

exec java -DBLOG_LOG_FILE="${LOG_FILE}" -jar /app/app.jar
