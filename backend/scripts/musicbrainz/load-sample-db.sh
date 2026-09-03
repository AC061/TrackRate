#!/usr/bin/env bash
# Carga sample dump MusicBrainz y verifica musicbrainz.artist en musicbrainz_db.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# shellcheck source=mb-env.sh
source "$ROOT/scripts/musicbrainz/mb-env.sh"
cd "$ROOT"

count_artists() {
  docker compose exec -T db psql -U musicbrainz -d "$MB_DB" -tAc \
    "SELECT count(*) FROM ${MB_SCHEMA}.artist" 2>/dev/null || echo "0"
}

has_artist_table() {
  docker compose exec -T db psql -U musicbrainz -d "$MB_DB" -tAc \
    "SELECT 1 FROM information_schema.tables WHERE table_schema='${MB_SCHEMA}' AND table_name='artist' LIMIT 1" \
    2>/dev/null | grep -q 1
}

echo "==> Modo standalone..."
if ! docker compose run --rm --no-deps musicbrainz printenv MUSICBRAINZ_STANDALONE_SERVER 2>/dev/null | grep -q 1; then
  echo "ERROR: MUSICBRAINZ_STANDALONE_SERVER no activo. Revisa compose/musicbrainz-standalone.yml"
  exit 1
fi

count="$(count_artists)"
if [[ "$count" =~ ^[1-9][0-9]*$ ]]; then
  echo "OK  ${MB_DB}.${MB_SCHEMA}.artist ya tiene ${count} filas"
  exit 0
fi

LOG="$(mktemp /tmp/mb-createdb.XXXXXX.log)"
trap 'rm -f "$LOG"' EXIT

if has_artist_table; then
  echo "==> Tabla artist existe pero está vacía — recreando base (sin re-descargar dump)..."
  set +e
  docker compose run --rm musicbrainz recreatedb.sh -sample 2>&1 | tee "$LOG"
  CREATEDB_EXIT=${PIPESTATUS[0]}
  set -e
else
  echo "==> Cargando sample dump (~15 GB, puede tardar 30–90 min)..."
  set +e
  docker compose run --rm musicbrainz createdb.sh -sample -fetch 2>&1 | tee "$LOG"
  CREATEDB_EXIT=${PIPESTATUS[0]}
  set -e
fi

if [[ "$CREATEDB_EXIT" -ne 0 ]] || grep -q "InitDb.pl failed" "$LOG"; then
  echo "ERROR: falló la importación. Si hay dumps corruptos (wget 416), ejecuta:"
  echo "  ./scripts/musicbrainz/reset-db-volumes.sh"
  exit 1
fi

count="$(count_artists)"
if [[ "$count" =~ ^[1-9][0-9]*$ ]]; then
  echo "OK  ${MB_DB}.${MB_SCHEMA}.artist (${count} filas)"
  exit 0
fi

echo "ERROR: import terminó pero ${MB_SCHEMA}.artist sigue vacío en ${MB_DB}."
echo "       (No uses psql -d musicbrainz — la base correcta es ${MB_DB})"
exit 1
