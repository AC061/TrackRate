#!/usr/bin/env bash
# MusicBrainz Docker — mirror producción (~100–350 GB). Requiere token MetaBrainz para replicación.
set -euo pipefail

INSTALL_DIR="${1:-/opt/musicbrainz-docker}"

if [[ ! -d "$INSTALL_DIR/.git" ]]; then
  git clone https://github.com/metabrainz/musicbrainz-docker.git "$INSTALL_DIR"
fi

cd "$INSTALL_DIR"
docker compose build
docker compose run --rm musicbrainz createdb.sh -fetch

docker compose run --rm musicbrainz bash -c \
  'carton exec -- ./admin/BuildMaterializedTables --database=MAINTENANCE all'

docker compose up -d

echo "Opcional: índices Solr — docker compose exec indexer python -m sir reindex"
echo "Replicación: admin/set-replication-token && admin/configure add replication-token replication-cron"
