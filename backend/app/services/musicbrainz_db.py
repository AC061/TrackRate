"""Búsqueda y validación directa en Postgres MusicBrainz (sin Solr)."""

from __future__ import annotations

import logging
from uuid import UUID

from sqlalchemy import create_engine, text

from app.config import settings

logger = logging.getLogger(__name__)

_TABLE = {
    "artist": "artist",
    "album": "release_group",
    "track": "recording",
}

_engine = None


class MusicBrainzDbError(Exception):
    pass


def _get_engine():
    global _engine
    if _engine is None:
        _engine = create_engine(settings.musicbrainz_database_url, pool_pre_ping=True)
    return _engine


def _pattern(query: str) -> str:
    return f"%{query.strip()}%"


def ping() -> dict:
    """Comprueba conexión y devuelve conteos básicos."""
    try:
        with _get_engine().connect() as conn:
            artists = conn.execute(text("SELECT count(*) FROM artist")).scalar_one()
            albums = conn.execute(text("SELECT count(*) FROM release_group")).scalar_one()
            tracks = conn.execute(text("SELECT count(*) FROM recording")).scalar_one()
            return {
                "ok": True,
                "artists": int(artists),
                "albums": int(albums),
                "tracks": int(tracks),
            }
    except Exception as exc:
        logger.exception("MusicBrainz Postgres no accesible")
        return {"ok": False, "error": str(exc)}


def search(
    query: str,
    entity_type: str | None = None,
    *,
    limit: int = 25,
) -> list[dict]:
    clean = query.strip()
    if not clean:
        return []

    types = [entity_type] if entity_type else ["artist", "album", "track"]
    per_type = limit if entity_type else max(limit // len(types), 5)
    pattern = _pattern(clean)
    results: list[dict] = []

    try:
        with _get_engine().connect() as conn:
            for etype in types:
                if etype == "artist":
                    rows = conn.execute(
                        text(
                            """
                            SELECT gid::text, name, comment
                            FROM artist
                            WHERE name ILIKE :q
                            ORDER BY name
                            LIMIT :lim
                            """
                        ),
                        {"q": pattern, "lim": per_type},
                    )
                    for gid, name, comment in rows:
                        item: dict = {"id": gid, "name": name, "_trackrate_type": "artist"}
                        if comment:
                            item["disambiguation"] = comment
                        results.append(item)
                elif etype == "album":
                    rows = conn.execute(
                        text(
                            """
                            SELECT gid::text, name, comment
                            FROM release_group
                            WHERE name ILIKE :q
                            ORDER BY name
                            LIMIT :lim
                            """
                        ),
                        {"q": pattern, "lim": per_type},
                    )
                    for gid, name, comment in rows:
                        item = {"id": gid, "title": name, "_trackrate_type": "album"}
                        if comment:
                            item["disambiguation"] = comment
                        results.append(item)
                else:
                    rows = conn.execute(
                        text(
                            """
                            SELECT gid::text, name, length
                            FROM recording
                            WHERE name ILIKE :q
                            ORDER BY name
                            LIMIT :lim
                            """
                        ),
                        {"q": pattern, "lim": per_type},
                    )
                    for gid, name, length in rows:
                        item = {"id": gid, "title": name, "_trackrate_type": "track"}
                        if length is not None:
                            item["length"] = int(length)
                        results.append(item)
                if entity_type is not None:
                    break
    except Exception as exc:
        raise MusicBrainzDbError("No se pudo consultar MusicBrainz Postgres") from exc

    return results[:limit]


def exists(entity_type: str, mbid: UUID) -> bool:
    table = _TABLE.get(entity_type)
    if table is None:
        return False
    try:
        with _get_engine().connect() as conn:
            row = conn.execute(
                text(f"SELECT 1 FROM {table} WHERE gid = :gid LIMIT 1"),
                {"gid": str(mbid)},
            ).first()
            return row is not None
    except Exception:
        return False
