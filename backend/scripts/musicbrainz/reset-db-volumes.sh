#!/usr/bin/env bash
# Borra volúmenes Postgres + dumps MB y recarga sample dump desde cero.
# Usar cuando createdb falla con "schema already exists" o wget 416.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

echo "==> Parando contenedores..."
docker compose down

echo "==> Eliminando volúmenes MusicBrainz (pgdata + dbdump)..."
for vol in $(docker volume ls -q | grep -E 'pgdata|dbdump'); do
  echo "    rm $vol"
  docker volume rm "$vol" || true
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

echo "==> Descargando e importando sample dump (30–90 min). Usa tmux/screen."
docker compose run --rm musicbrainz createdb.sh -sample -fetch

echo "==> Verificando artist..."
if docker compose exec -T db psql -U musicbrainz -d musicbrainz -tAc \
  "SELECT count(*) FROM musicbrainz.artist" 2>/dev/null | grep -qE '^[1-9]'; then
  echo "OK  musicbrainz.artist cargado"
elif docker compose exec -T db psql -U musicbrainz -d musicbrainz -tAc \
  "SELECT count(*) FROM artist" 2>/dev/null | grep -qE '^[1-9]'; then
  echo "OK  public.artist cargado"
else
  echo "ERROR: dump terminó pero artist sigue vacío"
  exit 1
fi

echo "==> Levantando stack..."
docker compose up -d

echo "Listo. curl http://localhost:8000/catalog/mb-status"
