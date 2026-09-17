"""Búsqueda y validación directa en Postgres MusicBrainz (sin Solr)."""

from __future__ import annotations

import logging
from uuid import UUID

from sqlalchemy import create_engine, text

from app.config import settings

logger = logging.getLogger(__name__)

_ENTITY_TABLE = {
    "artist": "artist",
    "album": "release_group",
    "track": "recording",
}

_engine = None
_schema: str | None = None


class MusicBrainzDbError(Exception):
    pass


def _get_engine():
    global _engine
    if _engine is None:
        _engine = create_engine(settings.musicbrainz_database_url, pool_pre_ping=True)
    return _engine


def _resolve_schema(*, force: bool = False) -> str:
    global _schema
    if _schema is not None and not force:
        return _schema

    with create_engine(settings.musicbrainz_database_url, pool_pre_ping=True).connect() as conn:
        row = conn.execute(
            text(
                """
                SELECT table_schema
                FROM information_schema.tables
                WHERE table_name = 'artist'
                ORDER BY
                  CASE table_schema
                    WHEN 'musicbrainz' THEN 0
                    WHEN 'public' THEN 1
                    ELSE 2
                  END
                LIMIT 1
                """
            )
        ).first()

    if row is None:
        raise MusicBrainzDbError(
            "Tabla artist no encontrada. ¿Terminó createdb.sh? "
            "Ejecuta: docker compose run --rm musicbrainz createdb.sh -sample -fetch"
        )

    _schema = row[0]
    return _schema


def _qualified(table: str) -> str:
    schema = _resolve_schema()
    return f"{schema}.{table}"


def _pattern(query: str) -> str:
    return f"%{query.strip()}%"


def ping() -> dict:
    """Comprueba conexión y devuelve conteos básicos."""
    try:
        schema = _resolve_schema(force=True)
        with _get_engine().connect() as conn:
            artists = conn.execute(
                text(f"SELECT count(*) FROM {_qualified('artist')}")
            ).scalar_one()
            albums = conn.execute(
                text(f"SELECT count(*) FROM {_qualified('release_group')}")
            ).scalar_one()
            tracks = conn.execute(
                text(f"SELECT count(*) FROM {_qualified('recording')}")
            ).scalar_one()
            return {
                "ok": True,
                "schema": schema,
                "artists": int(artists),
                "albums": int(albums),
                "tracks": int(tracks),
            }
    except MusicBrainzDbError as exc:
        return {"ok": False, "error": str(exc)}
    except Exception as exc:
        logger.exception("MusicBrainz Postgres no accesible")
        return {"ok": False, "error": str(exc)}


def search(
    query: str,
    entity_type: str | None = None,
    *,
    limit: int = 25,
    prefix_only: bool = False,
) -> list[dict]:
    clean = query.strip()
    if not clean:
        return []

    _resolve_schema()
    types = [entity_type] if entity_type else ["artist", "album", "track"]
    per_type = limit if entity_type else max(limit // len(types), 5)
    pattern = f"{clean}%" if prefix_only else _pattern(clean)
    results: list[dict] = []

    artist_t = _qualified("artist")
    album_t = _qualified("release_group")
    track_t = _qualified("recording")

    try:
        with _get_engine().connect() as conn:
            if prefix_only:
                conn.execute(text("SET statement_timeout = '10s'"))
            for etype in types:
                if etype == "artist":
                    rows = conn.execute(
                        text(
                            f"""
                            SELECT gid::text, name, comment
                            FROM {artist_t}
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
                            f"""
                            SELECT gid::text, name, comment
                            FROM {album_t}
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
                            f"""
                            SELECT gid::text, name, length
                            FROM {track_t}
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
    except MusicBrainzDbError:
        raise
    except Exception as exc:
        raise MusicBrainzDbError("No se pudo consultar MusicBrainz Postgres") from exc

    return results[:limit]


def fetch_by_ids(entity_type: str, mbids: list[str]) -> list[dict]:
    """Carga filas por MBID preservando el orden de mbids."""
    if not mbids:
        return []

    table = _ENTITY_TABLE.get(entity_type)
    if table is None:
        return []

    _resolve_schema()
    qualified = _qualified(table)
    unique = list(dict.fromkeys(mbid for mbid in mbids if mbid))

    try:
        with _get_engine().connect() as conn:
            if entity_type == "artist":
                rows = conn.execute(
                    text(
                        f"""
                        SELECT gid::text, name, comment
                        FROM {qualified}
                        WHERE gid::text = ANY(:ids)
                        """
                    ),
                    {"ids": unique},
                )
                by_id = {
                    gid: {
                        "id": gid,
                        "name": name,
                        "_trackrate_type": "artist",
                        **({"disambiguation": comment} if comment else {}),
                    }
                    for gid, name, comment in rows
                }
            elif entity_type == "album":
                rows = conn.execute(
                    text(
                        f"""
                        SELECT gid::text, name, comment
                        FROM {qualified}
                        WHERE gid::text = ANY(:ids)
                        """
                    ),
                    {"ids": unique},
                )
                by_id = {
                    gid: {
                        "id": gid,
                        "title": name,
                        "_trackrate_type": "album",
                        **({"disambiguation": comment} if comment else {}),
                    }
                    for gid, name, comment in rows
                }
            else:
                rows = conn.execute(
                    text(
                        f"""
                        SELECT gid::text, name, length
                        FROM {qualified}
                        WHERE gid::text = ANY(:ids)
                        """
                    ),
                    {"ids": unique},
                )
                by_id = {}
                for gid, name, length in rows:
                    item: dict = {"id": gid, "title": name, "_trackrate_type": "track"}
                    if length is not None:
                        item["length"] = int(length)
                    by_id[gid] = item
    except MusicBrainzDbError:
        raise
    except Exception as exc:
        raise MusicBrainzDbError("No se pudo consultar MusicBrainz Postgres") from exc

    return [by_id[mbid] for mbid in mbids if mbid in by_id]


def prefix_search(
    query: str,
    entity_type: str | None = None,
    *,
    limit: int = 10,
) -> list[str]:
    """Fallback de suggest: nombres que empiezan por el prefijo (ILIKE 'q%')."""
    clean = query.strip()
    if not clean:
        return []

    _resolve_schema()
    types = [entity_type] if entity_type else ["artist", "album", "track"]
    per_type = limit if entity_type else max(limit // len(types), 3)
    pattern = f"{clean}%"
    names: list[str] = []
    seen: set[str] = set()

    try:
        with _get_engine().connect() as conn:
            for etype in types:
                table = _qualified(_ENTITY_TABLE[etype])
                rows = conn.execute(
                    text(
                        f"""
                        SELECT DISTINCT name
                        FROM {table}
                        WHERE name ILIKE :q
                        ORDER BY name
                        LIMIT :lim
                        """
                    ),
                    {"q": pattern, "lim": per_type},
                )
                for (name,) in rows:
                    key = name.lower()
                    if key not in seen:
                        seen.add(key)
                        names.append(name)
                if entity_type is not None:
                    break
    except MusicBrainzDbError:
        raise
    except Exception as exc:
        raise MusicBrainzDbError("No se pudo consultar MusicBrainz Postgres") from exc

    return names[:limit]


def exists(entity_type: str, mbid: UUID) -> bool:
    table = _ENTITY_TABLE.get(entity_type)
    if table is None:
        return False
    try:
        qualified = _qualified(table)
        with _get_engine().connect() as conn:
            row = conn.execute(
                text(f"SELECT 1 FROM {qualified} WHERE gid = :gid LIMIT 1"),
                {"gid": str(mbid)},
            ).first()
            return row is not None
    except Exception:
        return False
