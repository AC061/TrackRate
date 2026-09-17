#!/usr/bin/env bash
# Levanta Sonic y verifica que trackrate-api lo resuelve por DNS.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

NETWORK="${TRACKRATE_DOCKER_NETWORK:-trackrate-stack_default}"

echo "==> Levantando sonic + trackrate-api..."
docker compose up -d sonic trackrate-api

echo "==> Estado sonic:"
if ! docker compose ps sonic | grep -qE 'Up|running'; then
  echo "ERROR: sonic no está Up"
  docker compose logs sonic --tail 50
  exit 1
fi

echo "==> Conectando sonic y trackrate-api a $NETWORK (por si quedaron fuera)..."
for name in sonic trackrate-api; do
  if docker ps --format '{{.Names}}' | grep -qx "$name"; then
    docker network connect "$NETWORK" "$name" 2>/dev/null && echo "    conectado $name" || echo "    $name ya en red"
  fi
done

echo "==> Esperando healthcheck sonic (máx. 60 s)..."
for i in $(seq 1 30); do
  status="$(docker inspect sonic --format '{{.State.Health.Status}}' 2>/dev/null || echo unknown)"
  if [[ "$status" == "healthy" ]]; then
    echo "    sonic healthy"
    break
  fi
  if [[ "$i" -eq 30 ]]; then
    echo "WARN: sonic sin healthcheck healthy (status=$status) — revisando logs:"
    docker compose logs sonic --tail 30
  fi
  sleep 2
done

echo "==> DNS desde trackrate-api:"
if docker compose exec -T trackrate-api getent hosts sonic; then
  echo "OK"
else
  echo "ERROR: sonic no resuelve DNS dentro de trackrate-api"
  echo ""
  echo "Diagnóstico:"
  docker compose ps sonic trackrate-api
  docker network inspect "$NETWORK" --format '{{range .Containers}}{{.Name}} {{end}}' 2>/dev/null || true
  echo ""
  echo "Intenta:"
  echo "  docker compose up -d --force-recreate --no-deps sonic trackrate-api"
  exit 1
fi

echo "==> Ping Sonic desde API..."
docker compose exec -T trackrate-api python -m scripts.diagnose_sonic
