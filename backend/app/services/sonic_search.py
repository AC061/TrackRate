"""Búsqueda de catálogo vía Sonic (typos + suggest) con enriquecimiento Postgres."""

from __future__ import annotations

import logging
import time

from app.config import settings
from app.services.musicbrainz_db import MusicBrainzDbError, fetch_by_ids, prefix_search
from app.services.sonic_client import SonicClient, SonicError
from app.services.search_rank import rerank_search_results
from app.services.sonic_hosts import (
    clear_working_host_cache,
    remember_working_host,
    resolve_sonic_host_for_query,
    sonic_host_candidates,
)

logger = logging.getLogger(__name__)

_ENTITY_BUCKETS = ("artist", "album", "track")


class SonicSearchError(Exception):
    pass


def _client(*, timeout: float | None = None) -> SonicClient:
    """Cliente listo para QUERY. Resolución con probe corto; lectura con timeout de búsqueda."""
    read_timeout = timeout or settings.sonic_query_timeout_seconds
    try:
        host = resolve_sonic_host_for_query()
    except SonicError as exc:
        raise SonicSearchError(str(exc)) from exc

    client = SonicClient(host=host, timeout=read_timeout)
    probe = SonicClient(host=host, timeout=settings.sonic_probe_timeout_seconds)
    if not probe.ping():
        clear_working_host_cache()
        raise SonicSearchError(f"Sonic no responde PING en {host}")
    remember_working_host(host)
    return client


def ping() -> dict:
    if not settings.sonic_enabled:
        return {"ok": False, "enabled": False, "error": "Sonic desactivado"}

    probe_timeout = settings.sonic_probe_timeout_seconds
    last_error = "sin host"
    host_used: str | None = None

    for host in sonic_host_candidates():
        client = SonicClient(host=host, timeout=probe_timeout)
        try:
            if not client.ping():
                continue
            host_used = host
            counts: dict[str, int] = {}
            with client.session("ingest") as ingest:
                for bucket in _ENTITY_BUCKETS:
                    try:
                        counts[bucket] = ingest.count(settings.sonic_collection, bucket)
                    except SonicError:
                        counts[bucket] = 0
            t0 = time.perf_counter()
            sample_ids = client.query(
                settings.sonic_collection,
                "artist",
                "the",
                limit=3,
                lang="eng",
            )
            probe = {
                "query_ms": round((time.perf_counter() - t0) * 1000, 1),
                "sample_artist_ids": sample_ids,
                "indexed": any(counts.get(b, 0) > 0 for b in _ENTITY_BUCKETS),
            }
            remember_working_host(host)
            return {
                "ok": True,
                "enabled": True,
                "host": settings.sonic_host,
                "host_used": host_used,
                "collection": settings.sonic_collection,
                "indexed_terms": counts,
                "note": "indexed_terms cuenta palabras en el índice, no objetos MB",
                "probe": probe,
            }
        except (OSError, SonicError, TimeoutError) as exc:
            last_error = str(exc)
            continue

    return {"ok": False, "enabled": True, "error": last_error}


def _buckets(entity_type: str | None) -> list[str]:
    if entity_type in _ENTITY_BUCKETS:
        return [entity_type]
    return list(_ENTITY_BUCKETS)


def search(
    query: str,
    entity_type: str | None = None,
    *,
    limit: int = 25,
) -> list[dict]:
    """Devuelve dicts enriquecidos desde Postgres, ordenados por relevancia Sonic."""
    if not settings.sonic_enabled:
        raise SonicSearchError("Sonic desactivado")

    clean = query.strip()
    if not clean:
        return []

    buckets = _buckets(entity_type)
    # Over-fetch: Sonic ordena por fuzzy score; re-ranking elige "The Beatles" sobre "Beatles Ranked".
    fetch_limit = min(max(limit * 10, 50), 200)
    per_bucket = fetch_limit if entity_type else max(fetch_limit // len(buckets), 20)
    collection = settings.sonic_collection

    ordered_ids: list[tuple[str, str]] = []
    seen: set[str] = set()

    t0 = time.perf_counter()
    host: str | None = None
    try:
        client = _client()
        host = client.host
        logger.info("sonic search usando host=%s q=%r", host, clean)
        with client.session("search") as session:
            for bucket in buckets:
                ids = session._query(collection, bucket, clean, limit=per_bucket, lang="eng")
                for mbid in ids:
                    key = f"{bucket}:{mbid}"
                    if key not in seen:
                        seen.add(key)
                        ordered_ids.append((bucket, mbid))
    except (TimeoutError, OSError, SonicError) as exc:
        clear_working_host_cache()
        raise SonicSearchError(f"Sonic no disponible ({host}): {exc}") from exc
    sonic_ms = (time.perf_counter() - t0) * 1000
    logger.info("sonic query q=%r buckets=%s ids=%d ms=%.0f", clean, buckets, len(ordered_ids), sonic_ms)

    if not ordered_ids:
        return []

    results: list[dict] = []
    by_bucket: dict[str, list[str]] = {b: [] for b in buckets}
    for bucket, mbid in ordered_ids:
        by_bucket[bucket].append(mbid)

    fetched: dict[str, dict] = {}
    t1 = time.perf_counter()
    for bucket, ids in by_bucket.items():
        if not ids:
            continue
        try:
            for row in fetch_by_ids(bucket, ids):
                fetched[f"{bucket}:{row['id']}"] = row
        except MusicBrainzDbError as exc:
            logger.warning("Enriquecimiento Postgres falló para %s: %s", bucket, exc)
    pg_ms = (time.perf_counter() - t1) * 1000
    logger.info("sonic enrich rows=%d ms=%.0f", len(fetched), pg_ms)

    for bucket, mbid in ordered_ids:
        row = fetched.get(f"{bucket}:{mbid}")
        if row is not None:
            results.append(row)

    return rerank_search_results(clean, results, limit=limit)


def suggest(
    word: str,
    entity_type: str | None = None,
    *,
    limit: int = 10,
) -> list[str]:
    if not settings.sonic_enabled:
        raise SonicSearchError("Sonic desactivado")

    clean = word.strip()
    if not clean:
        return []

    buckets = _buckets(entity_type)
    per_bucket = limit if entity_type else max(limit // len(buckets), 3)
    collection = settings.sonic_collection

    suggestions: list[str] = []
    seen: set[str] = set()

    try:
        with _client().session("search") as client:
            for bucket in buckets:
                for term in client._suggest(collection, bucket, clean, limit=per_bucket, lang="eng"):
                    lower = term.lower()
                    if lower not in seen:
                        seen.add(lower)
                        suggestions.append(term)
    except (TimeoutError, OSError, SonicError) as exc:
        if settings.sonic_fallback_sql:
            logger.warning("Sonic suggest falló, fallback SQL prefix: %s", exc)
            return prefix_search(clean, entity_type, limit=limit)
        raise SonicSearchError(f"Sonic no disponible: {exc}") from exc

    return suggestions[:limit]
