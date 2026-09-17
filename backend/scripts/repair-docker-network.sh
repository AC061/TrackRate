#!/usr/bin/env bash
# Repara contenedores del stack que no están en trackrate-stack_default.
# Útil tras añadir compose/network.yml o si falla:
#   "container ... is not connected to the network trackrate-stack_default"
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

NETWORK="${TRACKRATE_DOCKER_NETWORK:-trackrate-stack_default}"

echo "==> Contenedores huérfanos que bloquean la red..."
# indexer ya no corre por defecto (profile manual-index); contenedores viejos impiden recrear la red
for orphan in trackrate-stack-indexer-1 trackrate-indexer-1; do
  if docker ps -a --format '{{.Names}}' | grep -qx "$orphan"; then
    echo "    Eliminando $orphan (indexador Solr no usado por TrackRate)..."
    docker rm -f "$orphan" 2>/dev/null || true
  fi
done

echo "==> Red objetivo: $NETWORK"
if ! docker network inspect "$NETWORK" >/dev/null 2>&1; then
  echo "    Creando red..."
  docker network create "$NETWORK"
fi

echo "==> Conectando contenedores del proyecto a $NETWORK..."
connected=0
skipped=0
while IFS= read -r cid; do
  [[ -z "$cid" ]] && continue
  name="$(docker inspect "$cid" --format '{{.Name}}' | sed 's/^\///')"
  if docker network inspect "$NETWORK" --format '{{range .Containers}}{{.Name}} {{end}}' | grep -qE "(^| )${name}( |$)"; then
    skipped=$((skipped + 1))
    continue
  fi
  if docker network connect "$NETWORK" "$cid" 2>/dev/null; then
    echo "    + $name"
    connected=$((connected + 1))
  else
    echo "    ? $name (no se pudo conectar — puede estar detenido)"
  fi
done < <(docker compose ps -aq 2>/dev/null || true)

echo "    Conectados: $connected, ya en red: $skipped"

echo "==> Levantando sonic + trackrate-api (--no-deps, sin tocar MusicBrainz)..."
docker compose up -d sonic
docker network connect "$NETWORK" sonic 2>/dev/null || true
docker network connect "$NETWORK" trackrate-api 2>/dev/null || true
docker compose up -d --force-recreate --no-deps trackrate-api

sleep 3
echo "==> DNS sonic desde trackrate-api:"
if docker compose exec -T trackrate-api getent hosts sonic; then
  echo "OK"
else
  echo "ERROR: sonic sigue sin resolver."
  echo "Contenedores en $NETWORK:"
  docker network inspect "$NETWORK" --format '{{range .Containers}}  {{.Name}} {{end}}'
  exit 1
fi
