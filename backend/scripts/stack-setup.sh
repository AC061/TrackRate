#!/usr/bin/env bash
# Setup completo: TrackRate + MusicBrainz (sample dump ~15 GB).
# Ejecutar desde backend/: ./scripts/stack-setup.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MB_DIR="${MUSICBRAINZ_DOCKER_DIR:-$ROOT/musicbrainz-docker}"
SAMPLE="${1:-sample}"  # sample | full

cd "$ROOT"

if [[ ! -d "$MB_DIR/.git" ]]; then
  echo "==> Clonando musicbrainz-docker en $MB_DIR..."
  git clone https://github.com/metabrainz/musicbrainz-docker.git "$MB_DIR"
fi

cd "$MB_DIR"
echo "==> Configurando musicbrainz-standalone en clone MB (opcional)..."
admin/configure add musicbrainz-standalone 2>/dev/null || true

cd "$ROOT"
echo "==> Construyendo imágenes (MusicBrainz + TrackRate)..."
docker compose build

if [[ "$SAMPLE" == "sample" ]]; then
  echo "==> Verificando modo standalone (requerido para -sample)..."
  if ! docker compose run --rm --no-deps musicbrainz printenv MUSICBRAINZ_STANDALONE_SERVER 2>/dev/null | grep -q 1; then
    echo "ERROR: MUSICBRAINZ_STANDALONE_SERVER no está activo."
    echo "       Asegúrate de tener compose/musicbrainz-standalone.yml incluido en docker-compose.yml"
    exit 1
  fi
fi

echo "==> Creando base MusicBrainz (puede tardar mucho)..."
if [[ "$SAMPLE" == "sample" ]]; then
  docker compose run --rm musicbrainz createdb.sh -sample -fetch
else
  docker compose run --rm musicbrainz createdb.sh -fetch
fi

# shellcheck source=musicbrainz/mb-env.sh
source "$ROOT/scripts/musicbrainz/mb-env.sh"
count=$(docker compose exec -T db psql -U musicbrainz -d "$MB_DB" -tAc \
  "SELECT count(*) FROM ${MB_SCHEMA}.artist" 2>/dev/null || echo "0")
if ! [[ "$count" =~ ^[1-9][0-9]*$ ]]; then
  echo "ERROR: ${MB_DB}.${MB_SCHEMA}.artist vacío tras createdb (count=${count})"
  exit 1
fi
echo "==> MusicBrainz OK (${count} artistas en ${MB_DB})"

if [[ ! -f .env ]] && [[ -f .env.example ]]; then
  cp .env.example .env
  echo "==> Creado .env desde .env.example"
fi

echo "==> Levantando stack completo..."
docker compose up -d

echo ""
echo "Listo."
echo "  MusicBrainz WS: http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo localhost):5000/ws/2"
echo "  TrackRate API:  http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo localhost):8000"
echo "  Docs:           http://localhost:8000/docs"
echo ""
echo "Logs: docker compose logs -f trackrate-api musicbrainz"
