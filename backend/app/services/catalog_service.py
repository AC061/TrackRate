from datetime import datetime, timezone
from uuid import UUID

from sqlalchemy import func, or_, select
from sqlalchemy.orm import Session

from app.models import (
    Album,
    Artist,
    CatalogContributor,
    CONTRIBUTOR_ROLE_LABELS,
    ContributorRole,
    ModerationStatus,
    MusicEntityType,
    Profile,
    Rating,
    Track,
    TrackSample,
)
from app.schemas.catalog import (
    CatalogDetailResponse,
    CatalogItemResponse,
    ContributorInput,
    ContributorResponse,
    SampleInput,
    SampleResponse,
    TopRatedTrackResponse,
)
from app.services import label_service
from app.services.storage_service import normalize_stored_media_url


class CatalogNotFoundError(Exception):
    pass


class CatalogValidationError(Exception):
    pass


SEARCH_LIMIT = 50
MAX_CONTRIBUTORS = 20
MAX_SAMPLES = 10
TOP_RATED_LIMIT = 20


def _submission_moderation(db: Session, user_id: UUID) -> tuple[ModerationStatus, UUID | None, datetime | None]:
    profile = db.get(Profile, user_id)
    if profile is not None and profile.is_admin:
        return ModerationStatus.APPROVED, user_id, datetime.now(timezone.utc)
    return ModerationStatus.PENDING, None, None


def _artist_item(artist: Artist) -> CatalogItemResponse:
    return CatalogItemResponse(
        id=artist.id,
        type="artist",
        title=artist.name,
        subtitle=None,
        image_url=normalize_stored_media_url(artist.image_url),
        year=None,
    )


def _album_item(album: Album, artist_name: str) -> CatalogItemResponse:
    return CatalogItemResponse(
        id=album.id,
        type="album",
        title=album.title,
        subtitle=artist_name,
        image_url=normalize_stored_media_url(album.cover_url),
        year=album.release_year,
    )


def _track_item(track: Track, artist_name: str, cover_url: str | None) -> CatalogItemResponse:
    return CatalogItemResponse(
        id=track.id,
        type="track",
        title=track.title,
        subtitle=artist_name,
        image_url=normalize_stored_media_url(cover_url),
        year=None,
    )


def _load_contributors(
    db: Session,
    entity_type: MusicEntityType,
    entity_id: UUID,
) -> list[ContributorResponse]:
    stmt = (
        select(CatalogContributor, Artist.name)
        .join(Artist, Artist.id == CatalogContributor.artist_id)
        .where(
            CatalogContributor.entity_type == entity_type,
            CatalogContributor.entity_id == entity_id,
        )
        .order_by(CatalogContributor.sort_order, Artist.name)
    )
    return [
        ContributorResponse(
            artist_id=contributor.artist_id,
            artist_name=artist_name,
            role=contributor.role.value,
            role_label=CONTRIBUTOR_ROLE_LABELS[contributor.role],
            notes=contributor.notes,
        )
        for contributor, artist_name in db.execute(stmt)
    ]


def _load_samples(db: Session, track_id: UUID) -> list[SampleResponse]:
    stmt = (
        select(TrackSample, Track, Artist.name, Album.title)
        .join(Track, Track.id == TrackSample.sampled_track_id)
        .join(Artist, Artist.id == Track.artist_id)
        .outerjoin(Album, Album.id == Track.album_id)
        .where(TrackSample.track_id == track_id)
        .order_by(TrackSample.sort_order, Track.title)
    )
    return [
        SampleResponse(
            track_id=sampled_track.id,
            title=sampled_track.title,
            artist_name=artist_name,
            album_title=album_title,
            notes=sample.notes,
        )
        for sample, sampled_track, artist_name, album_title in db.execute(stmt)
    ]


def _validate_contributors(
    db: Session,
    contributors: list[ContributorInput],
    *,
    main_artist_id: UUID | None = None,
) -> None:
    if len(contributors) > MAX_CONTRIBUTORS:
        raise CatalogValidationError(f"Máximo {MAX_CONTRIBUTORS} contribuyentes por envío")

    seen: set[tuple[UUID, ContributorRole]] = set()
    for item in contributors:
        key = (item.artist_id, ContributorRole(item.role.value))
        if key in seen:
            raise CatalogValidationError("Contribuyente duplicado con el mismo rol")
        seen.add(key)

        artist = db.get(Artist, item.artist_id)
        if artist is None:
            raise CatalogValidationError("Contribuyente no encontrado")
        if artist.status != ModerationStatus.APPROVED:
            raise CatalogValidationError("El contribuyente debe ser un artista aprobado")
        if main_artist_id is not None and item.artist_id == main_artist_id:
            raise CatalogValidationError("El artista principal no puede ser contribuyente")


def _validate_samples(
    db: Session,
    samples: list[SampleInput],
    *,
    track_id: UUID | None = None,
) -> None:
    if len(samples) > MAX_SAMPLES:
        raise CatalogValidationError(f"Máximo {MAX_SAMPLES} samples por canción")

    seen: set[UUID] = set()
    for item in samples:
        if item.sampled_track_id in seen:
            raise CatalogValidationError("Sample duplicado")
        seen.add(item.sampled_track_id)

        if track_id is not None and item.sampled_track_id == track_id:
            raise CatalogValidationError("Una canción no puede muestrearse a sí misma")

        sampled = db.get(Track, item.sampled_track_id)
        if sampled is None:
            raise CatalogValidationError("Canción muestreada no encontrada")
        if sampled.status != ModerationStatus.APPROVED:
            raise CatalogValidationError("La canción muestreada debe estar aprobada")


def _save_contributors(
    db: Session,
    entity_type: MusicEntityType,
    entity_id: UUID,
    contributors: list[ContributorInput],
) -> None:
    for index, item in enumerate(contributors):
        db.add(
            CatalogContributor(
                entity_type=entity_type,
                entity_id=entity_id,
                artist_id=item.artist_id,
                role=ContributorRole(item.role.value),
                notes=item.notes,
                sort_order=index,
            )
        )


def _save_samples(db: Session, track_id: UUID, samples: list[SampleInput]) -> None:
    for index, item in enumerate(samples):
        db.add(
            TrackSample(
                track_id=track_id,
                sampled_track_id=item.sampled_track_id,
                notes=item.notes,
                sort_order=index,
            )
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
            image_url=normalize_stored_media_url(artist.image_url),
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
            description=album.description,
            image_url=normalize_stored_media_url(album.cover_url),
            year=album.release_year,
            artist_id=album.artist_id,
            label=label_service.get_label_name(db, album.label_id),
            label_id=album.label_id,
            contributors=_load_contributors(db, MusicEntityType.ALBUM, album.id),
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
            description=track.description,
            image_url=normalize_stored_media_url(cover_url or track.cover_url),
            duration_ms=track.duration_ms,
            artist_id=track.artist_id,
            album_id=track.album_id,
            label=label_service.get_label_name(db, track.label_id),
            label_id=track.label_id,
            contributors=_load_contributors(db, MusicEntityType.TRACK, track.id),
            samples=_load_samples(db, track.id),
        )

    raise CatalogValidationError("Tipo de catálogo no válido")


def build_album_detail_response(db: Session, album: Album) -> CatalogDetailResponse:
    artist_name = db.scalar(select(Artist.name).where(Artist.id == album.artist_id))
    return CatalogDetailResponse(
        id=album.id,
        type="album",
        title=album.title,
        subtitle=artist_name,
        description=album.description,
        image_url=normalize_stored_media_url(album.cover_url),
        year=album.release_year,
        artist_id=album.artist_id,
        label=label_service.get_label_name(db, album.label_id),
        label_id=album.label_id,
        status=album.status.value,
        contributors=_load_contributors(db, MusicEntityType.ALBUM, album.id),
    )


def build_track_detail_response(db: Session, track: Track) -> CatalogDetailResponse:
    row = db.execute(
        select(Artist.name, Album.title, Album.cover_url)
        .select_from(Track)
        .join(Artist, Artist.id == Track.artist_id)
        .outerjoin(Album, Album.id == Track.album_id)
        .where(Track.id == track.id)
    ).first()
    artist_name, album_title, cover_url = row if row else (None, None, None)
    return CatalogDetailResponse(
        id=track.id,
        type="track",
        title=track.title,
        subtitle=artist_name,
        extra=album_title,
        description=track.description,
        image_url=cover_url or track.cover_url,
        duration_ms=track.duration_ms,
        artist_id=track.artist_id,
        album_id=track.album_id,
        label=label_service.get_label_name(db, track.label_id),
        label_id=track.label_id,
        status=track.status.value,
        contributors=_load_contributors(db, MusicEntityType.TRACK, track.id),
        samples=_load_samples(db, track.id),
    )


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


def build_artist_detail_response(db: Session, artist: Artist) -> CatalogDetailResponse:
    return CatalogDetailResponse(
        id=artist.id,
        type="artist",
        title=artist.name,
        description=artist.bio,
        image_url=normalize_stored_media_url(artist.image_url),
        status=artist.status.value,
    )


def get_submission_detail(db: Session, entity_type: str, entity_id: UUID) -> CatalogDetailResponse:
    if entity_type == "artist":
        artist = db.get(Artist, entity_id)
        if artist is None:
            raise CatalogNotFoundError("Artista no encontrado")
        return build_artist_detail_response(db, artist)
    if entity_type == "album":
        album = db.get(Album, entity_id)
        if album is None:
            raise CatalogNotFoundError("Álbum no encontrado")
        return build_album_detail_response(db, album)
    if entity_type == "track":
        track = db.get(Track, entity_id)
        if track is None:
            raise CatalogNotFoundError("Canción no encontrada")
        return build_track_detail_response(db, track)
    raise CatalogValidationError("Tipo de catálogo no válido")


def top_rated_tracks(db: Session, limit: int = TOP_RATED_LIMIT) -> list[TopRatedTrackResponse]:
    stmt = (
        select(
            Track,
            Artist.name,
            Album.cover_url,
            func.round(func.avg(Rating.rating), 2),
            func.count(),
        )
        .join(Artist, Artist.id == Track.artist_id)
        .outerjoin(Album, Album.id == Track.album_id)
        .join(Rating, (Rating.entity_id == Track.id) & (Rating.entity_type == MusicEntityType.TRACK))
        .where(
            Track.status == ModerationStatus.APPROVED,
            Artist.status == ModerationStatus.APPROVED,
            or_(Track.album_id.is_(None), Album.status == ModerationStatus.APPROVED),
        )
        .group_by(Track.id, Artist.name, Album.cover_url)
        .having(func.count() >= 1)
        .order_by(func.avg(Rating.rating).desc(), func.count().desc(), Track.title)
        .limit(limit)
    )
    results: list[TopRatedTrackResponse] = []
    for track, artist_name, cover_url, average, count in db.execute(stmt):
        results.append(
            TopRatedTrackResponse(
                id=track.id,
                title=track.title,
                subtitle=artist_name,
                image_url=normalize_stored_media_url(cover_url or track.cover_url),
                average_rating=float(average),
                rating_count=int(count),
            )
        )
    return results


def submit_artist(db: Session, user_id: UUID, name: str, bio: str | None) -> Artist:
    status, reviewed_by, reviewed_at = _submission_moderation(db, user_id)
    artist = Artist(
        name=name.strip(),
        bio=bio,
        submitted_by=user_id,
        status=status,
        reviewed_by=reviewed_by,
        reviewed_at=reviewed_at,
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
    description: str | None = None,
    label_id: UUID | None = None,
    contributors: list[ContributorInput] | None = None,
) -> Album:
    artist = db.get(Artist, artist_id)
    if artist is None:
        raise CatalogValidationError("Artista no encontrado")

    try:
        label_service.validate_label_id(db, label_id)
    except label_service.LabelNotFoundError as exc:
        raise CatalogValidationError(str(exc)) from exc

    contributor_items = contributors or []
    _validate_contributors(db, contributor_items, main_artist_id=artist_id)
    status, reviewed_by, reviewed_at = _submission_moderation(db, user_id)

    album = Album(
        title=title.strip(),
        artist_id=artist_id,
        release_year=release_year,
        description=description,
        label_id=label_id,
        submitted_by=user_id,
        status=status,
        reviewed_by=reviewed_by,
        reviewed_at=reviewed_at,
    )
    db.add(album)
    db.flush()
    _save_contributors(db, MusicEntityType.ALBUM, album.id, contributor_items)
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
    description: str | None = None,
    label_id: UUID | None = None,
    contributors: list[ContributorInput] | None = None,
    samples: list[SampleInput] | None = None,
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

    try:
        label_service.validate_label_id(db, label_id)
    except label_service.LabelNotFoundError as exc:
        raise CatalogValidationError(str(exc)) from exc

    contributor_items = contributors or []
    sample_items = samples or []
    _validate_contributors(db, contributor_items, main_artist_id=artist_id)
    _validate_samples(db, sample_items)
    status, reviewed_by, reviewed_at = _submission_moderation(db, user_id)

    track = Track(
        title=title.strip(),
        artist_id=artist_id,
        album_id=album_id,
        duration_ms=duration_ms,
        description=description,
        label_id=label_id,
        submitted_by=user_id,
        status=status,
        reviewed_by=reviewed_by,
        reviewed_at=reviewed_at,
    )
    db.add(track)
    db.flush()
    _validate_samples(db, sample_items, track_id=track.id)
    _save_contributors(db, MusicEntityType.TRACK, track.id, contributor_items)
    _save_samples(db, track.id, sample_items)
    db.commit()
    db.refresh(track)
    return track
