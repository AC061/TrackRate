#!/usr/bin/env python3
"""Smoke test end-to-end contra la API TrackRate."""

from __future__ import annotations

import os
import sys

import httpx

BASE_URL = os.environ.get("TRACKRATE_API_URL", "http://localhost:8000").rstrip("/")
MB_URL = os.environ.get("MUSICBRAINZ_WS_URL", "http://localhost:5000/ws/2").rstrip("/")


def check(name: str, ok: bool, detail: str = "") -> None:
    status = "OK" if ok else "FAIL"
    suffix = f" — {detail}" if detail else ""
    print(f"[{status}] {name}{suffix}")
    if not ok:
        sys.exit(1)


def main() -> None:
    with httpx.Client(timeout=10.0) as client:
        health = client.get(f"{BASE_URL}/health")
        check("GET /health", health.status_code == 200, health.text)

        openapi = client.get(f"{BASE_URL}/openapi.json")
        check("OpenAPI disponible", openapi.status_code == 200)
        paths = openapi.json().get("paths", {})
        check("/catalog/search en OpenAPI", "/catalog/search" in paths)
        check("/entities/top-rated en OpenAPI", "/entities/top-rated" in paths)
        check("Sin POST /catalog/artists", "/catalog/artists" not in paths)

        search = client.get(f"{BASE_URL}/catalog/search", params={"q": "beatles", "type": "artist"})
        check("GET /catalog/search", search.status_code == 200, f"{len(search.json())} resultados")

        top = client.get(f"{BASE_URL}/entities/top-rated", params={"type": "track"})
        check("GET /entities/top-rated", top.status_code == 200)

    try:
        with httpx.Client(timeout=5.0) as client:
            mb = client.get(f"{MB_URL}/artist", params={"query": "beatles", "fmt": "json", "limit": 1})
            check("MusicBrainz WS accesible", mb.status_code == 200, MB_URL)
    except httpx.HTTPError:
        print("[WARN] MusicBrainz WS no accesible — levanta musicbrainz-docker para búsqueda real")

    print("\nSmoke test completado.")


if __name__ == "__main__":
    main()
