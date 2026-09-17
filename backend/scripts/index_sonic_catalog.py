"""Indexa el catálogo MusicBrainz en Sonic (artistas, álbumes, pistas).

Ejecutar tras cargar el sample dump:
  cd backend && python -m scripts.index_sonic_catalog

Desde Docker:
  docker compose exec trackrate-api python -m scripts.index_sonic_catalog
"""

from __future__ import annotations

import argparse
import logging
import sys
import time

from sqlalchemy import text

from app.config import settings
from app.services.musicbrainz_db import MusicBrainzDbError, _get_engine, _qualified, _resolve_schema
from app.services.sonic_client import SonicClient, SonicError
from app.services.sonic_hosts import resolve_sonic_host, sonic_host_candidates

logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)

BATCH_SIZE = 500

_ENTITIES = (
    ("artist", "artist", ("gid::text", "name", "comment", "sort_name")),
    ("album", "release_group", ("gid::text", "name", "comment")),
    ("track", "recording", ("gid::text", "name", None)),
)


def _search_text(name: str, comment: str | None, sort_name: str | None = None) -> str:
    """Texto indexado en Sonic: nombre, sort_name y variantes sin artículo inicial."""
    parts: list[str] = []
    seen: set[str] = set()

    def add(value: str | None) -> None:
        if not value or not value.strip():
            return
        text = value.strip()
        key = text.casefold()
        if key in seen:
            return
        seen.add(key)
        parts.append(text)
        if key.startswith("the "):
            alt = text[4:].strip()
            alt_key = alt.casefold()
            if alt and alt_key not in seen:
                seen.add(alt_key)
                parts.append(alt)

    add(name)
    add(sort_name)
    if comment and comment.strip():
        add(comment.strip())
    return " ".join(parts)


def _stream_rows(entity_type: str, table: str, columns: tuple):
    schema = _resolve_schema()
    qualified = _qualified(table)
    select_cols = ", ".join(c for c in columns if c is not None)
    sql = f"SELECT {select_cols} FROM {qualified} WHERE name IS NOT NULL AND name <> '' ORDER BY id"

    with _get_engine().connect() as conn:
        result = conn.execution_options(stream_results=True).execute(text(sql))
        batch: list[tuple[str, str]] = []
        total = 0

        for row in result:
            if entity_type == "artist":
                gid, name, comment, sort_name = row
                text_value = _search_text(name, comment, sort_name)
            elif entity_type == "album":
                gid, name, comment = row
                text_value = _search_text(name, comment)
            else:
                gid, name = row
                text_value = _search_text(name, None)

            if not text_value:
                continue

            batch.append((gid, text_value))
            if len(batch) >= BATCH_SIZE:
                yield batch
                total += len(batch)
                batch = []

        if batch:
            yield batch
            total += len(batch)

        logger.info("  %s.%s: %d objetos leídos", schema, table, total)


def index_entity(
    client: SonicClient,
    entity_type: str,
    table: str,
    columns: tuple,
    *,
    flush: bool,
) -> int:
    collection = settings.sonic_collection
    if flush:
        logger.info("==> FLUSHB %s/%s", collection, entity_type)
        client.flush_bucket(collection, entity_type)

    indexed = client.push_batches(
        collection,
        entity_type,
        _stream_rows(entity_type, table, columns),
        lang="eng",
    )

    logger.info("OK  %s: %d objetos indexados", entity_type, indexed)
    return indexed


def _wait_for_sonic(
    *,
    attempts: int = 30,
    delay: float = 2.0,
    timeout: float | None = None,
) -> SonicClient:
    sonic_timeout = timeout if timeout is not None else settings.sonic_timeout_seconds
    last_error = "sin host resoluble"

    for attempt in range(1, attempts + 1):
        resolved = resolve_sonic_host()
        hosts = [resolved] if resolved else sonic_host_candidates()
        for host in hosts:
            if not host:
                continue
            client = SonicClient(host=host, timeout=sonic_timeout)
            try:
                if client.ping(mode="ingest"):
                    if host != settings.sonic_host:
                        logger.info("Sonic accesible en %s (config: %s)", host, settings.sonic_host)
                    return client
            except OSError as exc:
                last_error = str(exc)
            except SonicError as exc:
                last_error = str(exc)

        logger.info(
            "Esperando Sonic (%s:%s) intento %d/%d — probados: %s",
            settings.sonic_host,
            settings.sonic_port,
            attempt,
            attempts,
            ", ".join(sonic_host_candidates()),
        )
        time.sleep(delay)

    logger.error(
        "Sonic no responde — %s\n"
        "¿Contenedor sonic Up?  docker compose ps sonic\n"
        "¿DNS desde API?       docker compose exec trackrate-api getent hosts sonic\n"
        "Recrear red/servicios:\n"
        "  docker compose up -d sonic\n"
        "  docker compose up -d --force-recreate trackrate-api\n"
        "O usa el wrapper:     ./scripts/index-sonic.sh --flush",
        last_error,
    )
    raise SystemExit(1)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Indexar catálogo MusicBrainz en Sonic")
    parser.add_argument(
        "--type",
        choices=["artist", "album", "track"],
        help="Indexar solo un tipo de entidad",
    )
    parser.add_argument(
        "--flush",
        action="store_true",
        help="Vaciar bucket antes de indexar",
    )
    parser.add_argument(
        "--no-consolidate",
        action="store_true",
        help="No ejecutar TRIGGER consolidate al final",
    )
    args = parser.parse_args(argv)

    try:
        _resolve_schema(force=True)
    except MusicBrainzDbError as exc:
        logger.error("MusicBrainz no listo: %s", exc)
        return 1

    targets = [e for e in _ENTITIES if args.type is None or e[0] == args.type]
    client = _wait_for_sonic(timeout=settings.sonic_timeout_seconds)

    started = time.perf_counter()
    total = 0

    try:
        for entity_type, table, columns in targets:
            logger.info("==> Indexando %s ...", entity_type)
            total += index_entity(
                client,
                entity_type,
                table,
                columns,
                flush=args.flush,
            )

        if not args.no_consolidate:
            logger.info("==> Consolidando índice Sonic (OK ≠ listo para buscar)...")
            client.consolidate()
            logger.info(
                "==> Esperando QUERY (FST rebuild puede tardar 5–15 min en sample MB)..."
            )
            try:
                ms = client.wait_for_search_ready(
                    settings.sonic_collection,
                    bucket="artist",
                    probe_term="beatles",
                    timeout_seconds=900,
                )
                logger.info("==> Sonic listo para búsquedas (probe %.0f ms)", ms)
            except SonicError as exc:
                logger.error(
                    "%s\n"
                    "Las búsquedas pueden hacer timeout hasta que termine. "
                    "Reintenta: python -m scripts.diagnose_sonic",
                    exc,
                )
                return 1
    except SonicError as exc:
        logger.error("Error Sonic: %s", exc)
        return 1

    elapsed = time.perf_counter() - started
    logger.info("Listo: %d objetos en %.1fs", total, elapsed)
    return 0


if __name__ == "__main__":
    sys.exit(main())
