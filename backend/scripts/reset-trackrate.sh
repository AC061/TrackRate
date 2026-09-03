#!/usr/bin/env bash
# Reset solo volúmenes TrackRate (sin tocar MusicBrainz).
set -euo pipefail
ROOT="$(ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT" && pwd)"
cd "$ROOT"

docker compose stop trackrate-api trackrate-postgres trackrate-minio 2>/dev/null || true
docker compose rm -f trackrate-api trackrate-postgres trackrate-minio 2>/dev/null || true
docker volume rm -f trackrate-stack_trackrate_postgres_data trackrate-stack_trackrate_minio_data 2>/dev/null || \
  docker volume rm -f trackrate_postgres_data trackrate_minio_data 2>/dev/null || true

docker compose up -d trackrate-postgres trackrate-minio trackrate-api
echo "TrackRate reiniciado. MusicBrainz no modificado."
