from uuid import UUID

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models import MusicEntityType, Rating
from app.schemas.catalog import CatalogDetailResponse, CatalogItemResponse, TopRatedEntityResponse
from app.services.cover_art_service import cover_art_url
from app.services.musicbrainz_client import (
    MusicBrainzClient,
    MusicBrainzError,
    MusicBrainzNotFoundError,
    artist_credit,
    duration_ms,
    first_release_date,
)

_mb = MusicBrainzClient()

SEARCH_LIMIT = 50
TOP_RATED_LIMIT = 50


class CatalogNotFoundError(Exception):
    pass


class CatalogValidationError(Exception):
    pass


def _parse_entity_type(raw: str) -> str:
    if raw not in ("artist", "album", "track"):
        raise CatalogValidationError("Tipo de entidad no válido")
    return raw


def _mbid(item: dict) -> UUID:
    return UUID(item["id"])


def _mb_item(raw: dict, entity_type: str) -> CatalogItemResponse:
    title = raw.get("title") or raw.get("name") or ""
    subtitle = None if entity_type == "artist" else artist_credit(raw)
    mbid = _mbid(raw)
    image = cover_art_url(entity_type, mbid)
    return CatalogItemResponse(
        id=mbid,
        type=entity_type,
        title=title,
        subtitle=subtitle,
        image_url=image,
        year=first_release_date(raw),
    )


def search_catalog(
    db: Session,
    query: str,
    entity_type: str | None = None,
) -> list[CatalogItemResponse]:
    del db
    clean = query.strip()
    if not clean:
        return []

    try:
        results = _mb.search(clean, entity_type, limit=SEARCH_LIMIT)
    except MusicBrainzError:
        return []

    items: list[CatalogItemResponse] = []
    for raw in results:
        etype = raw.pop("_trackrate_type", entity_type or "track")
        items.append(_mb_item(raw, etype))
    return items


def list_albums_by_artist(db: Session, artist_id: UUID) -> list[CatalogItemResponse]:
    del db
    try:
        data = _mb.lookup("artist", artist_id)
    except MusicBrainzNotFoundError as exc:
        raise CatalogNotFoundError("Artista no encontrado") from exc
    except MusicBrainzError as exc:
        raise CatalogNotFoundError("No se pudo cargar el artista") from exc

    release_groups = data.get("release-groups") or []
    return [_mb_item(rg, "album") for rg in release_groups]


def get_catalog_detail(
    db: Session,
    entity_type: str,
    entity_id: UUID,
) -> CatalogDetailResponse:
    del db
    parsed = _parse_entity_type(entity_type)

    try:
        data = _mb.lookup(parsed, entity_id)
    except MusicBrainzNotFoundError as exc:
        raise CatalogNotFoundError("Entidad no encontrada") from exc
    except MusicBrainzError as exc:
        raise CatalogNotFoundError("No se pudo cargar la entidad") from exc

    image = cover_art_url(parsed, entity_id)

    if parsed == "artist":
        return CatalogDetailResponse(
            id=entity_id,
            type="artist",
            title=data.get("name") or "",
            subtitle=None,
            extra=data.get("disambiguation"),
            description=None,
            image_url=image,
            year=None,
            duration_ms=None,
            artist_id=None,
            album_id=None,
        )

    if parsed == "album":
        artist_ref = None
        artist_name = artist_credit(data)
        ac = data.get("artist-credit") or []
        if ac and isinstance(ac, list):
            artist_obj = ac[0].get("artist")
            if artist_obj and artist_obj.get("id"):
                artist_ref = UUID(artist_obj["id"])

        return CatalogDetailResponse(
            id=entity_id,
            type="album",
            title=data.get("title") or "",
            subtitle=artist_name,
            extra=data.get("primary-type"),
            description=None,
            image_url=image,
            year=first_release_date(data),
            duration_ms=None,
            artist_id=artist_ref,
            album_id=None,
        )

    album_ref = None
    releases = data.get("releases") or []
    if releases:
        rg = releases[0].get("release-group")
        if rg and rg.get("id"):
            album_ref = UUID(rg["id"])
        elif not album_ref:
            release_id = releases[0].get("id")
            if release_id:
                image = cover_art_url("track", entity_id, release_mbid=UUID(release_id)) or image

    artist_ref = None
    ac = data.get("artist-credit") or []
    if ac and isinstance(ac, list):
        artist_obj = ac[0].get("artist")
        if artist_obj and artist_obj.get("id"):
            artist_ref = UUID(artist_obj["id"])

    return CatalogDetailResponse(
        id=entity_id,
        type="track",
        title=data.get("title") or "",
        subtitle=artist_credit(data),
        extra=None,
        description=None,
        image_url=image,
        year=first_release_date(data),
        duration_ms=duration_ms(data),
        artist_id=artist_ref,
        album_id=album_ref,
    )


def get_cover_url(entity_type: str, entity_id: UUID) -> str | None:
    parsed = _parse_entity_type(entity_type)
    return cover_art_url(parsed, entity_id)


def top_rated_entities(
    db: Session,
    entity_type: str,
    limit: int = 20,
) -> list[TopRatedEntityResponse]:
    parsed = _parse_entity_type(entity_type)
    mb_type = MusicEntityType(parsed)

    rows = db.execute(
        select(
            Rating.entity_id,
            func.round(func.avg(Rating.rating), 2),
            func.count(),
            func.max(Rating.entity_title),
            func.max(Rating.entity_subtitle),
            func.max(Rating.entity_image_url),
        )
        .where(Rating.entity_type == mb_type)
        .group_by(Rating.entity_id)
        .order_by(func.avg(Rating.rating).desc(), func.count().desc())
        .limit(min(limit, TOP_RATED_LIMIT))
    )

    results: list[TopRatedEntityResponse] = []
    for entity_id, average, count, title, subtitle, image in rows:
        if not title:
            title, subtitle, image_from_mb = fetch_snapshots_for_entity(mb_type, entity_id)
            image = image or image_from_mb
        results.append(
            TopRatedEntityResponse(
                id=entity_id,
                type=parsed,
                title=title or "Desconocido",
                subtitle=subtitle,
                image_url=image or cover_art_url(parsed, entity_id),
                average_rating=float(average),
                rating_count=int(count),
            )
        )
    return results


def fetch_snapshots_for_entity(
    entity_type: MusicEntityType,
    entity_id: UUID,
) -> tuple[str | None, str | None, str | None]:
    from app.services.entity_helpers import fetch_entity_snapshots

    title, subtitle, _ = fetch_entity_snapshots(entity_type, entity_id)
    image = cover_art_url(entity_type.value, entity_id)
    return title, subtitle, image
