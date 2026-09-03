#!/usr/bin/env bash
# Carga sample dump MusicBrainz y verifica que exista musicbrainz.artist.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

echo "==> Modo standalone..."
if ! docker compose run --rm --no-deps musicbrainz printenv MUSICBRAINZ_STANDALONE_SERVER 2>/dev/null | grep -q 1; then
  echo "ERROR: MUSICBRAINZ_STANDALONE_SERVER no activo. Revisa compose/musicbrainz-standalone.yml"
  exit 1
fi

echo "==> Cargando sample dump (~15 GB, puede tardar 30–90 min)..."
docker compose run --rm musicbrainz createdb.sh -sample -fetch

echo "==> Verificando tablas..."
for schema in musicbrainz public; do
  if docker compose exec -T db psql -U musicbrainz -d musicbrainz -tAc \
    "SELECT 1 FROM information_schema.tables WHERE table_schema='${schema}' AND table_name='artist' LIMIT 1" \
    | grep -q 1; then
    count=$(docker compose exec -T db psql -U musicbrainz -d musicbrainz -tAc \
      "SELECT count(*) FROM ${schema}.artist")
    echo "OK  ${schema}.artist existe (${count} filas)"
    exit 0
  fi
done

echo "ERROR: createdb terminó pero no hay tabla artist. Revisa logs del contenedor musicbrainz."
exit 1
