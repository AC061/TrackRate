#!/usr/bin/env bash
# MusicBrainz mirror producción (~100–350 GB).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
MB_DIR="${MUSICBRAINZ_DOCKER_DIR:-$ROOT/musicbrainz-docker}"

cd "$ROOT"

if [[ ! -d "$MB_DIR/.git" ]]; then
  git clone https://github.com/metabrainz/musicbrainz-docker.git "$MB_DIR"
fi

echo "==> Construyendo imágenes..."
docker compose build

echo "==> Creando base MusicBrainz (dump completo, horas)..."
docker compose run --rm musicbrainz createdb.sh -fetch

docker compose run --rm musicbrainz bash -c \
  'carton exec -- ./admin/BuildMaterializedTables --database=MAINTENANCE all'

docker compose up -d

echo "Opcional: docker compose exec indexer python -m sir reindex"
echo "Replicación: cd musicbrainz-docker && admin/set-replication-token"
echo "               admin/configure add replication-token replication-cron"
