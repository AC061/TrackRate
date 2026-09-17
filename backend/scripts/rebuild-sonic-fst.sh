#!/usr/bin/env bash
# Reconstruye el índice FST de Sonic (QUERY/SUGGEST) tras indexar millones de objetos.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "==> Recreando sonic (tcp_timeout=7200 en config)..."
docker compose up -d --force-recreate sonic
sleep 5

echo "==> COUNT (KV — debe mostrar términos > 0):"
docker compose exec -T trackrate-api python3 -c "
from app.config import settings
from app.services.sonic_client import SonicClient
c = SonicClient(timeout=30)
with c.session('ingest') as s:
    for b in ('artist', 'album', 'track'):
        print(f'  {b}:', s.count(settings.sonic_collection, b))
"

echo "==> TRIGGER consolidate..."
docker compose exec -T trackrate-api python3 -c "
from app.services.sonic_client import SonicClient, SonicError
from app.config import settings

c = SonicClient(timeout=60)
c.consolidate()
print('Consolidate OK — esperando QUERY (hasta 15 min)...')
ms = c.wait_for_search_ready(
    settings.sonic_collection,
    bucket='artist',
    probe_term='beatles',
    timeout_seconds=900,
    query_timeout=120,
)
print(f'Sonic QUERY listo ({ms:.0f} ms)')
"

echo "==> Diagnóstico:"
docker compose exec -T trackrate-api python -m scripts.diagnose_sonic
