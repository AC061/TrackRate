#!/usr/bin/env bash
# Diagnóstico: TrackRate Postgres vs MusicBrainz. Ejecutar en el servidor Ubuntu.
set -euo pipefail

HOST="${1:-100.126.35.7}"
MB_URL="${MUSICBRAINZ_WS_URL:-http://${HOST}:5000/ws/2}"
API_URL="${TRACKRATE_API_URL:-http://${HOST}:8000}"
UA="${MUSICBRAINZ_USER_AGENT:-TrackRate/1.0 (dev@trackrate.local)}"

echo "=== TrackRate API ($API_URL) ==="
if curl -sf "$API_URL/health" >/dev/null; then
  echo "OK  /health"
  curl -s "$API_URL/catalog/search?q=beatles&type=artist" | head -c 400
  echo ""
else
  echo "FAIL — ¿docker compose up en backend/?"
fi

echo ""
echo "=== TrackRate catalog search (SQL, sin Solr) ==="
if curl -sf "$API_URL/catalog/search?q=beatles&type=artist" | head -c 300; then
  echo ""
else
  echo "FAIL — revisa MUSICBRAINZ_DATABASE_URL y que db tenga datos"
fi

echo ""
echo "=== MusicBrainz WS search (puede estar vacío sin Solr; es normal) ==="
if curl -sf -H "User-Agent: $UA" "${MB_URL}/artist?query=beatles&fmt=json&limit=1" | head -c 200; then
  echo ""
  echo "OK  MusicBrainz responde con datos"
else
  echo "FAIL — MusicBrainz vacío o no instalado."
  echo "      Ejecuta: ./scripts/stack-setup.sh"
  echo "      (tarda ~15 GB descarga + createdb.sh -sample -fetch)"
fi

echo ""
echo "=== TrackRate Postgres (social) ==="
if docker ps --format '{{.Names}}' 2>/dev/null | grep -q postgres; then
  PG=$(docker ps --format '{{.Names}}' | grep postgres | head -1)
  docker exec "$PG" psql -U trackrate -d trackrate -c "
    SELECT 'users' AS tbl, count(*) FROM users
    UNION ALL SELECT 'ratings', count(*) FROM ratings
    UNION ALL SELECT 'lists', count(*) FROM lists;
  " 2>/dev/null || echo "No se pudo consultar Postgres"
  echo ""
  echo "Nota: el catálogo YA NO está en TrackRate. Solo users/ratings/lists."
  echo "      Sin ratings, /entities/top-rated estará vacío (normal)."
else
  echo "Contenedor postgres no encontrado"
fi

echo ""
echo "=== MusicBrainz Postgres (catálogo) ==="
if docker ps --format '{{.Names}}' 2>/dev/null | grep -qi musicbrainz; then
  MBDB=$(docker ps --format '{{.Names}}' | grep -E '^trackrate-stack-db-|^db-' | head -1 || true)
  if [[ -n "${MBDB:-}" ]]; then
    docker exec "$MBDB" psql -U musicbrainz -d musicbrainz -c \
      "SELECT count(*) AS artists FROM musicbrainz.artist;" 2>/dev/null || \
      docker exec "$MBDB" psql -U musicbrainz -d musicbrainz -c \
      "SELECT count(*) AS artists FROM artist;" 2>/dev/null || \
      echo "Consulta artist falló — ¿terminó createdb.sh?"
  fi
else
  echo "musicbrainz-docker no detectado. Clona e instala sample dump."
fi
