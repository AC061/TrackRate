#!/usr/bin/env python3
"""Diagnóstico Sonic: ping, count, QUERY con tiempos."""

from __future__ import annotations

import sys
import time

from app.config import settings
from app.services.sonic_client import SonicClient, SonicError
from app.services.sonic_hosts import (
    resolve_sonic_host_for_query,
    running_in_container,
    sonic_dns_help,
)


def main() -> int:
    in_container = running_in_container()
    read_timeout = settings.sonic_query_read_timeout_seconds
    print(f"host={settings.sonic_host}:{settings.sonic_port} collection={settings.sonic_collection}")
    print(f"query_read_timeout={read_timeout}s (api_fast={settings.sonic_query_timeout_seconds}s)")
    print(f"runtime={'container' if in_container else 'host'}")

    try:
        host = resolve_sonic_host_for_query()
    except SonicError as exc:
        print(f"\nERROR: {exc}")
        print(sonic_dns_help())
        return 1
    print(f"\n--- host: {host} (QUERY operativo) ---")

    client = SonicClient(host=host, timeout=read_timeout)
    t0 = time.perf_counter()
    if not client.ping():
        print("PING: FAIL")
        print(sonic_dns_help())
        return 1
    print(f"PING: OK ({(time.perf_counter() - t0) * 1000:.0f} ms)")

    with client.session("ingest") as ingest:
        for bucket in ("artist", "album", "track"):
            try:
                n = ingest.count(settings.sonic_collection, bucket)
                print(f"  COUNT {bucket}: {n} términos")
            except SonicError as exc:
                print(f"  COUNT {bucket}: ERROR {exc}")

    query_ok = False
    for term in ("beatles", "the"):
        t0 = time.perf_counter()
        try:
            ids = client.query(
                settings.sonic_collection,
                "artist",
                term,
                limit=5,
                lang="eng",
            )
            ms = (time.perf_counter() - t0) * 1000
            print(f"  QUERY artist {term!r}: {len(ids)} ids en {ms:.0f} ms → {ids[:3]}")
            query_ok = True
        except (TimeoutError, OSError, SonicError) as exc:
            ms = (time.perf_counter() - t0) * 1000
            print(f"  QUERY artist {term!r}: FAIL tras {ms:.0f} ms → {exc}")

    if not query_ok:
        print(
            "\nERROR: QUERY no respondió."
            "\nComprueba versión en contenedor:"
            "\n  docker compose exec trackrate-api grep read_timeout /app/app/config.py"
            "\nSi falta, sube archivos por FTP y:"
            "\n  docker compose build trackrate-api"
            "\n  docker compose up -d --force-recreate --no-deps trackrate-api"
        )
        return 1

    print("\nOK — Sonic listo para búsquedas.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
