#!/usr/bin/env bash
# Reset total: borra volúmenes TrackRate + MusicBrainz y vuelve a setup sample.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "==> Parando stack y eliminando volúmenes..."
docker compose down -v --remove-orphans

exec bash "$ROOT/scripts/stack-setup.sh" sample
