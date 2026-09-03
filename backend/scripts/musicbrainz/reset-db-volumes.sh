#!/usr/bin/env bash
# Borra volúmenes Postgres + dumps MB y recarga sample dump desde cero.
# Usar cuando createdb falla con "schema already exists", wget 416 o artist vacío.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# shellcheck source=mb-env.sh
source "$ROOT/scripts/musicbrainz/mb-env.sh"
cd "$ROOT"

count_artists() {
  docker compose exec -T db psql -U musicbrainz -d "$MB_DB" -tAc \
    "SELECT count(*) FROM ${MB_SCHEMA}.artist" 2>/dev/null || echo "0"
}

echo "==> Parando contenedores..."
docker compose down -v --remove-orphans 2>/dev/null || docker compose down

echo "==> Eliminando volúmenes MusicBrainz (pgdata + dbdump)..."
for vol in $(docker volume ls -q | grep -E 'pgdata|dbdump'); do
  echo "    rm $vol"
  docker volume rm "$vol" 2>/dev/null || true
done

echo "==> Volúmenes restantes:"
docker volume ls | grep trackrate-stack || true

echo ""
echo "==> Iniciando db, search, valkey..."
docker compose up -d db search valkey

echo "==> Esperando Postgres MB..."
for i in $(seq 1 30); do
  if docker compose exec -T db pg_isready -U musicbrainz >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

echo "==> Verificando standalone..."
if ! docker compose run --rm --no-deps musicbrainz printenv MUSICBRAINZ_STANDALONE_SERVER 2>/dev/null | grep -q 1; then
  echo "ERROR: MUSICBRAINZ_STANDALONE_SERVER debe ser 1"
  exit 1
fi

LOG="$(mktemp /tmp/mb-createdb.XXXXXX.log)"
trap 'rm -f "$LOG"' EXIT

echo "==> Descargando e importando sample dump (30–90 min). Usa tmux/screen."
echo "    Log: $LOG"
set +e
docker compose run --rm musicbrainz createdb.sh -sample -fetch 2>&1 | tee "$LOG"
CREATEDB_EXIT=${PIPESTATUS[0]}
set -e

if [[ "$CREATEDB_EXIT" -ne 0 ]]; then
  echo "ERROR: createdb.sh salió con código $CREATEDB_EXIT"
  exit 1
fi

if grep -q "InitDb.pl failed" "$LOG"; then
  echo "ERROR: InitDb.pl failed — estado parcial. Prueba:"
  echo "  docker compose run --rm musicbrainz recreatedb.sh -sample"
  exit 1
fi

echo "==> Verificando ${MB_DB}.${MB_SCHEMA}.artist ..."
count="$(count_artists)"
if [[ "$count" =~ ^[1-9][0-9]*$ ]]; then
  echo "OK  ${count} artistas en ${MB_DB}.${MB_SCHEMA}.artist"
else
  echo "ERROR: dump terminó pero ${MB_SCHEMA}.artist tiene ${count} filas"
  echo ""
  echo "Diagnóstico rápido:"
  docker compose exec -T db psql -U musicbrainz -d postgres -c "\l" 2>/dev/null || true
  docker compose exec -T db psql -U musicbrainz -d "$MB_DB" -c "\dn" 2>/dev/null || true
  docker compose exec -T db psql -U musicbrainz -d "$MB_DB" -c \
    "SELECT table_schema, table_name FROM information_schema.tables WHERE table_name='artist';" \
    2>/dev/null || true
  echo ""
  echo "Si el dump existe pero InitDb falló antes:"
  echo "  docker compose run --rm musicbrainz recreatedb.sh -sample"
  exit 1
fi

echo "==> Levantando stack..."
docker compose up -d

echo "Listo. curl http://localhost:8000/catalog/mb-status"
