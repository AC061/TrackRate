#!/usr/bin/env bash
# Setup completo del stack TrackRate + MusicBrainz (sample dump ~15 GB).
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
echo "==> Configurando musicbrainz-standalone (dev/test)..."
admin/configure add musicbrainz-standalone

cd "$ROOT"
echo "==> Construyendo imágenes (MusicBrainz + TrackRate)..."
docker compose build

echo "==> Creando base MusicBrainz (puede tardar mucho)..."
if [[ "$SAMPLE" == "sample" ]]; then
  docker compose run --rm musicbrainz createdb.sh -sample -fetch
else
  docker compose run --rm musicbrainz createdb.sh -fetch
fi

if [[ ! -f backend/.env ]] && [[ -f backend/.env.example ]]; then
  cp backend/.env.example backend/.env
  echo "==> Creado backend/.env desde .env.example"
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
