#!/bin/sh
set -eu

LOG_ROOT="${BLOG_LOG_ROOT:-/logs}"
LOG_DATE="$(date +%F)"
LOG_DIR="${LOG_ROOT}/${LOG_DATE}"
LOG_FILE="${LOG_DIR}/llm-bridge.txt"

mkdir -p "${LOG_DIR}"
touch "${LOG_FILE}"

export BRIDGE_LOG_FILE="${LOG_FILE}"

exec python /app/server.py
