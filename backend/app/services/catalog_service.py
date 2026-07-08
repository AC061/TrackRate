from uuid import UUID

from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from app.models import Album, Artist, ModerationStatus, Track
from app.schemas.catalog import CatalogDetailResponse, CatalogItemResponse


class CatalogNotFoundError(Exception):
    pass


class CatalogValidationError(Exception):
    pass


SEARCH_LIMIT = 50


def _artist_item(artist: Artist) -> CatalogItemResponse:
    return CatalogItemResponse(
        id=artist.id,
        type="artist",
        title=artist.name,
        subtitle=None,
        image_url=artist.image_url,
        year=None,
    )


def _album_item(album: Album, artist_name: str) -> CatalogItemResponse:
    return CatalogItemResponse(
        id=album.id,
        type="album",
        title=album.title,
        subtitle=artist_name,
        image_url=album.cover_url,
        year=album.release_year,
    )


def _track_item(track: Track, artist_name: str, cover_url: str | None) -> CatalogItemResponse:
    return CatalogItemResponse(
        id=track.id,
        type="track",
        title=track.title,
        subtitle=artist_name,
        image_url=cover_url,
        year=None,
    )


def search_catalog(
    db: Session,
    query: str,
    entity_type: str | None,
) -> list[CatalogItemResponse]:
    trimmed = query.strip()
    pattern = f"%{trimmed}%" if trimmed else None
    results: list[CatalogItemResponse] = []

    if entity_type in (None, "artist"):
        stmt = select(Artist).where(Artist.status == ModerationStatus.APPROVED)
        if pattern:
            stmt = stmt.where(Artist.name.ilike(pattern))
        stmt = stmt.order_by(Artist.name).limit(SEARCH_LIMIT)
        results.extend(_artist_item(a) for a in db.scalars(stmt))

    if entity_type in (None, "album"):
        stmt = (
            select(Album, Artist.name)
            .join(Artist, Artist.id == Album.artist_id)
            .where(
                Album.status == ModerationStatus.APPROVED,
                Artist.status == ModerationStatus.APPROVED,
            )
        )
        if pattern:
            stmt = stmt.where(Album.title.ilike(pattern))
        stmt = stmt.order_by(Album.title).limit(SEARCH_LIMIT)
        results.extend(_album_item(album, artist_name) for album, artist_name in db.execute(stmt))

    if entity_type in (None, "track"):
        stmt = (
            select(Track, Artist.name, Album.cover_url)
            .join(Artist, Artist.id == Track.artist_id)
            .outerjoin(Album, Album.id == Track.album_id)
            .where(
                Track.status == ModerationStatus.APPROVED,
                Artist.status == ModerationStatus.APPROVED,
                or_(Track.album_id.is_(None), Album.status == ModerationStatus.APPROVED),
            )
        )
        if pattern:
            stmt = stmt.where(Track.title.ilike(pattern))
        stmt = stmt.order_by(Track.title).limit(SEARCH_LIMIT)
        results.extend(
            _track_item(track, artist_name, cover_url)
            for track, artist_name, cover_url in db.execute(stmt)
        )

    return sorted(results, key=lambda item: item.title.lower())


def get_catalog_detail(db: Session, entity_type: str, entity_id: UUID) -> CatalogDetailResponse:
    if entity_type == "artist":
        artist = db.scalar(
            select(Artist).where(
                Artist.id == entity_id,
                Artist.status == ModerationStatus.APPROVED,
            )
        )
        if artist is None:
            raise CatalogNotFoundError("Artista no encontrado")
        return CatalogDetailResponse(
            id=artist.id,
            type="artist",
            title=artist.name,
            description=artist.bio,
            image_url=artist.image_url,
        )

    if entity_type == "album":
        row = db.execute(
            select(Album, Artist.name)
            .join(Artist, Artist.id == Album.artist_id)
            .where(
                Album.id == entity_id,
                Album.status == ModerationStatus.APPROVED,
                Artist.status == ModerationStatus.APPROVED,
            )
        ).first()
        if row is None:
            raise CatalogNotFoundError("Álbum no encontrado")
        album, artist_name = row
        return CatalogDetailResponse(
            id=album.id,
            type="album",
            title=album.title,
            subtitle=artist_name,
            image_url=album.cover_url,
            year=album.release_year,
            artist_id=album.artist_id,
        )

    if entity_type == "track":
        row = db.execute(
            select(Track, Artist.name, Album.title, Album.cover_url)
            .join(Artist, Artist.id == Track.artist_id)
            .outerjoin(Album, Album.id == Track.album_id)
            .where(
                Track.id == entity_id,
                Track.status == ModerationStatus.APPROVED,
                Artist.status == ModerationStatus.APPROVED,
                or_(Track.album_id.is_(None), Album.status == ModerationStatus.APPROVED),
            )
        ).first()
        if row is None:
            raise CatalogNotFoundError("Canción no encontrada")
        track, artist_name, album_title, cover_url = row
        return CatalogDetailResponse(
            id=track.id,
            type="track",
            title=track.title,
            subtitle=artist_name,
            extra=album_title,
            image_url=cover_url or track.cover_url,
            duration_ms=track.duration_ms,
            artist_id=track.artist_id,
            album_id=track.album_id,
        )

    raise CatalogValidationError("Tipo de catálogo no válido")


def list_albums_by_artist(db: Session, artist_id: UUID) -> list[CatalogItemResponse]:
    stmt = (
        select(Album, Artist.name)
        .join(Artist, Artist.id == Album.artist_id)
        .where(
            Album.artist_id == artist_id,
            Album.status == ModerationStatus.APPROVED,
            Artist.status == ModerationStatus.APPROVED,
        )
        .order_by(Album.title)
        .limit(SEARCH_LIMIT)
    )
    return [_album_item(album, artist_name) for album, artist_name in db.execute(stmt)]


def submit_artist(db: Session, user_id: UUID, name: str, bio: str | None) -> Artist:
    artist = Artist(
        name=name.strip(),
        bio=bio,
        submitted_by=user_id,
        status=ModerationStatus.PENDING,
    )
    db.add(artist)
    db.commit()
    db.refresh(artist)
    return artist


def submit_album(
    db: Session,
    user_id: UUID,
    title: str,
    artist_id: UUID,
    release_year: int | None,
) -> Album:
    artist = db.get(Artist, artist_id)
    if artist is None:
        raise CatalogValidationError("Artista no encontrado")

    album = Album(
        title=title.strip(),
        artist_id=artist_id,
        release_year=release_year,
        submitted_by=user_id,
        status=ModerationStatus.PENDING,
    )
    db.add(album)
    db.commit()
    db.refresh(album)
    return album


def submit_track(
    db: Session,
    user_id: UUID,
    title: str,
    artist_id: UUID,
    album_id: UUID | None,
    duration_ms: int | None,
) -> Track:
    artist = db.get(Artist, artist_id)
    if artist is None:
        raise CatalogValidationError("Artista no encontrado")

    if album_id is not None:
        album = db.get(Album, album_id)
        if album is None:
            raise CatalogValidationError("Álbum no encontrado")
        if album.artist_id != artist_id:
            raise CatalogValidationError("El álbum no pertenece al artista indicado")

    track = Track(
        title=title.strip(),
        artist_id=artist_id,
        album_id=album_id,
        duration_ms=duration_ms,
        submitted_by=user_id,
        status=ModerationStatus.PENDING,
    )
    db.add(track)
    db.commit()
    db.refresh(track)
    return track
