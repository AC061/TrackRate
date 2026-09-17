#!/usr/bin/env bash
# Diagnóstico Docker: qué contenedor está Exited y por qué.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "=== docker compose ps -a ==="
docker compose ps -a 2>/dev/null || docker compose ps -a

echo ""
echo "=== Contenedores Exited (últimas 40 líneas de log cada uno) ==="
exited=0
while IFS= read -r line; do
  [[ -z "$line" ]] && continue
  name="${line%% *}"
  state="${line#* }"
  echo ""
  echo "--- $name ($state) ---"
  docker logs "$name" --tail 40 2>&1 || true
  exited=$((exited + 1))
done < <(
  docker compose ps -a --format '{{.Name}} {{.State}}' 2>/dev/null \
    | grep -i exited || true
)

if [[ "$exited" -eq 0 ]]; then
  echo "(ningún contenedor en estado Exited)"
fi

echo ""
echo "=== Servicios críticos ==="
for svc in db search valkey musicbrainz sonic trackrate-postgres trackrate-api; do
  if docker compose ps "$svc" 2>/dev/null | grep -qE 'Up|running'; then
    echo "OK   $svc"
  else
    echo "FAIL $svc"
    cid="$(docker compose ps -aq "$svc" 2>/dev/null | head -1 || true)"
    if [[ -n "${cid:-}" ]]; then
      docker logs "$cid" --tail 15 2>&1 | sed 's/^/       /' || true
    fi
  fi
done

echo ""
echo "=== Disco ==="
df -h . | tail -1

echo ""
echo "=== Siguiente paso ==="
echo "Pega la salida completa de este script si necesitas ayuda."
echo "Logs en vivo: docker compose logs -f db sonic trackrate-api"
