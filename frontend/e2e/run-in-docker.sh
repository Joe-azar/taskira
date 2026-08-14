#!/bin/sh
set -eu

target_host="${TASKIRA_E2E_HOST:-host.docker.internal}"

socat TCP-LISTEN:4200,fork,reuseaddr "TCP:${target_host}:4200" &
frontend_proxy_pid=$!
socat TCP-LISTEN:8080,fork,reuseaddr "TCP:${target_host}:8080" &
backend_proxy_pid=$!

cleanup() {
  kill "$frontend_proxy_pid" "$backend_proxy_pid" 2>/dev/null || true
}

trap cleanup EXIT INT TERM

npm run test:e2e -- "$@"
