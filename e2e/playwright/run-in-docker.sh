#!/bin/sh
set -eu

frontend_host="${TASKIRA_E2E_FRONTEND_HOST:-frontend}"
backend_host="${TASKIRA_E2E_BACKEND_HOST:-backend}"

socat TCP-LISTEN:4200,fork,reuseaddr "TCP:${frontend_host}:4200" &
frontend_proxy_pid=$!
socat TCP-LISTEN:8080,fork,reuseaddr "TCP:${backend_host}:8080" &
backend_proxy_pid=$!

cleanup() {
  kill "$frontend_proxy_pid" "$backend_proxy_pid" 2>/dev/null || true
}

trap cleanup EXIT INT TERM

wait_for_url() {
  name="$1"
  url="$2"

  attempt=1
  while [ "$attempt" -le 90 ]; do
    if curl --fail --silent --show-error "$url" >/dev/null 2>&1; then
      printf '%s is ready\n' "$name"
      return 0
    fi
    attempt=$((attempt + 1))
    sleep 2
  done

  printf 'Timed out waiting for %s at %s\n' "$name" "$url" >&2
  return 1
}

wait_for_url frontend http://localhost:4200/
wait_for_url backend http://localhost:8080/v3/api-docs

/workspace/node_modules/.bin/playwright test \
  --config /workspace/e2e/playwright/playwright.config.ts \
  "$@"
