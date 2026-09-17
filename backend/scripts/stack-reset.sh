#!/usr/bin/env bash
# Reset total: contenedores, redes atascadas, volúmenes TrackRate + MusicBrainz + Sonic.
# Luego reinstala el sample dump (~30–90 min) e indexa Sonic.
#
# Uso: ./scripts/stack-reset.sh
#      ./scripts/stack-reset.sh --skip-mb   # solo Docker, sin recargar dump MB
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

NETWORK="${TRACKRATE_DOCKER_NETWORK:-trackrate-stack_default}"
SKIP_MB=false
for arg in "$@"; do
  case "$arg" in
    --skip-mb) SKIP_MB=true ;;
    -h|--help)
      echo "Uso: $0 [--skip-mb]"
      echo "  --skip-mb  Borra contenedores/red/volúmenes y levanta stack sin recargar dump MB"
      exit 0
      ;;
  esac
done

echo "=============================================="
echo "  RESET COMPLETO TrackRate + MusicBrainz"
echo "  Se borrarán TODOS los volúmenes del stack."
echo "  El sample dump tardará 30–90 minutos."
echo "=============================================="
echo ""
read -r -p "¿Continuar? [y/N] " confirm
if [[ ! "$confirm" =~ ^[yY]$ ]]; then
  echo "Cancelado."
  exit 0
fi

echo ""
echo "==> 1/5 Parando stack..."
docker compose down --remove-orphans 2>/dev/null || true

echo "==> 2/5 Eliminando contenedores huérfanos..."
docker compose ps -aq 2>/dev/null | xargs -r docker rm -f 2>/dev/null || true
for name in \
  trackrate-stack-indexer-1 trackrate-indexer-1 \
  trackrate-api trackrate-postgres trackrate-minio \
  trackrate-stack-sonic-1 trackrate-sonic \
  trackrate-stack-musicbrainz-1 trackrate-stack-db-1 trackrate-stack-search-1
do
  docker rm -f "$name" 2>/dev/null || true
done

echo "==> 3/5 Liberando red $NETWORK (si está bloqueada)..."
if docker network inspect "$NETWORK" >/dev/null 2>&1; then
  mapfile -t endpoints < <(
    docker network inspect "$NETWORK" --format '{{range $k, $v := .Containers}}{{$v.Name}}{{"\n"}}{{end}}' 2>/dev/null || true
  )
  for ep in "${endpoints[@]}"; do
    [[ -z "$ep" ]] && continue
    echo "    desconectando $ep"
    docker network disconnect -f "$NETWORK" "$ep" 2>/dev/null || docker rm -f "$ep" 2>/dev/null || true
  done
  docker network rm "$NETWORK" 2>/dev/null || true
fi

echo "==> 4/5 Eliminando volúmenes del proyecto..."
docker compose down -v --remove-orphans 2>/dev/null || true
for vol in $(docker volume ls -q | grep -E '^trackrate-stack_' || true); do
  echo "    rm volume $vol"
  docker volume rm "$vol" 2>/dev/null || true
done
for vol in $(docker volume ls -q | grep -E 'pgdata|dbdump' || true); do
  echo "    rm volume $vol"
  docker volume rm "$vol" 2>/dev/null || true
done

echo "==> Volúmenes restantes (debería estar vacío o sin trackrate-stack_*):"
docker volume ls | grep -E 'trackrate|pgdata|dbdump|sonic' || echo "    (ninguno)"

echo ""
echo "==> 5/5 Reinstalando..."
if [[ "$SKIP_MB" == true ]]; then
  docker compose build
  docker compose up -d
  echo ""
  echo "Stack levantado (dump MB no recargado)."
  echo "Indexa Sonic: ./scripts/index-sonic.sh --flush"
else
  exec bash "$ROOT/scripts/stack-setup.sh" sample
fi
