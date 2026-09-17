#!/usr/bin/env bash
# Indexa catálogo en Sonic con comprobaciones de red Docker.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "==> Levantando Sonic..."
docker compose up -d sonic trackrate-api

echo "==> Estado Sonic:"
if ! docker compose ps sonic | grep -qE 'Up|running'; then
  echo "ERROR: contenedor sonic no está Up"
  docker compose logs sonic --tail 40
  exit 1
fi

echo "==> Comprobando DNS desde trackrate-api..."
if ! docker compose exec -T trackrate-api getent hosts sonic >/dev/null 2>&1; then
  echo "WARN: 'sonic' no resuelve — reparando red Docker..."
  if [[ -x "$(dirname "$0")/repair-docker-network.sh" ]]; then
    "$(dirname "$0")/repair-docker-network.sh"
  else
    docker network connect "${TRACKRATE_DOCKER_NETWORK:-trackrate-stack_default}" sonic 2>/dev/null || true
    docker network connect "${TRACKRATE_DOCKER_NETWORK:-trackrate-stack_default}" trackrate-api 2>/dev/null || true
    docker compose up -d --force-recreate --no-deps sonic trackrate-api
    sleep 5
  fi
fi

if ! docker compose exec -T trackrate-api getent hosts sonic >/dev/null 2>&1; then
  echo "ERROR: trackrate-api no resuelve hostname 'sonic'."
  echo "       docker compose exec trackrate-api getent hosts sonic"
  echo "       docker network inspect trackrate-stack_default"
  exit 1
fi

echo "==> Indexando (puede tardar mucho)..."
exec docker compose exec -T trackrate-api python -m scripts.index_sonic_catalog "$@"
