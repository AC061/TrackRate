from datetime import datetime, timezone
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models import Album, Artist, ModerationStatus, Profile, Track
from app.schemas.catalog import CatalogSubmissionResponse


class ModerationError(Exception):
    pass


class EntityNotFoundError(Exception):
    pass


def _submission(
    entity_id: UUID,
    entity_type: str,
    title: str,
    status: ModerationStatus,
    subtitle: str | None = None,
    rejection_reason: str | None = None,
) -> CatalogSubmissionResponse:
    return CatalogSubmissionResponse(
        id=entity_id,
        type=entity_type,
        title=title,
        subtitle=subtitle,
        status=status.value,
        rejection_reason=rejection_reason,
    )


def list_pending_submissions(db: Session) -> list[CatalogSubmissionResponse]:
    submissions: list[CatalogSubmissionResponse] = []

    artists = db.scalars(
        select(Artist)
        .where(Artist.status == ModerationStatus.PENDING)
        .order_by(Artist.created_at.desc())
    )
    submissions.extend(
        _submission(a.id, "artist", a.name, a.status, rejection_reason=a.rejection_reason)
        for a in artists
    )

    album_rows = db.execute(
        select(Album, Artist.name)
        .join(Artist, Artist.id == Album.artist_id)
        .where(Album.status == ModerationStatus.PENDING)
        .order_by(Album.created_at.desc())
    )
    submissions.extend(
        _submission(album.id, "album", album.title, album.status, artist_name, album.rejection_reason)
        for album, artist_name in album_rows
    )

    track_rows = db.execute(
        select(Track, Artist.name)
        .join(Artist, Artist.id == Track.artist_id)
        .where(Track.status == ModerationStatus.PENDING)
        .order_by(Track.created_at.desc())
    )
    submissions.extend(
        _submission(track.id, "track", track.title, track.status, artist_name, track.rejection_reason)
        for track, artist_name in track_rows
    )

    return submissions


def list_user_submissions(db: Session, user_id: UUID) -> list[CatalogSubmissionResponse]:
    submissions: list[CatalogSubmissionResponse] = []

    artists = db.scalars(
        select(Artist)
        .where(Artist.submitted_by == user_id)
        .order_by(Artist.created_at.desc())
    )
    submissions.extend(
        _submission(a.id, "artist", a.name, a.status, rejection_reason=a.rejection_reason)
        for a in artists
    )

    album_rows = db.execute(
        select(Album, Artist.name)
        .join(Artist, Artist.id == Album.artist_id)
        .where(Album.submitted_by == user_id)
        .order_by(Album.created_at.desc())
    )
    submissions.extend(
        _submission(album.id, "album", album.title, album.status, artist_name, album.rejection_reason)
        for album, artist_name in album_rows
    )

    track_rows = db.execute(
        select(Track, Artist.name)
        .join(Artist, Artist.id == Track.artist_id)
        .where(Track.submitted_by == user_id)
        .order_by(Track.created_at.desc())
    )
    submissions.extend(
        _submission(track.id, "track", track.title, track.status, artist_name, track.rejection_reason)
        for track, artist_name in track_rows
    )

    return submissions


def _get_entity(db: Session, entity_type: str, entity_id: UUID):
    if entity_type == "artist":
        return db.get(Artist, entity_id)
    if entity_type == "album":
        return db.get(Album, entity_id)
    if entity_type == "track":
        return db.get(Track, entity_id)
    return None


def moderate_entity(
    db: Session,
    admin_id: UUID,
    entity_type: str,
    entity_id: UUID,
    action: str,
    rejection_reason: str | None,
) -> CatalogSubmissionResponse:
    entity = _get_entity(db, entity_type, entity_id)
    if entity is None:
        raise EntityNotFoundError("Entrada no encontrada")

    if entity.status != ModerationStatus.PENDING:
        raise ModerationError("Solo se pueden moderar entradas pendientes")

    now = datetime.now(timezone.utc)
    if action == "approve":
        entity.status = ModerationStatus.APPROVED
        entity.rejection_reason = None
    elif action == "reject":
        if not rejection_reason or not rejection_reason.strip():
            raise ModerationError("Se requiere un motivo de rechazo")
        entity.status = ModerationStatus.REJECTED
        entity.rejection_reason = rejection_reason.strip()
    else:
        raise ModerationError("Acción no válida")

    entity.reviewed_by = admin_id
    entity.reviewed_at = now
    db.commit()
    db.refresh(entity)

    subtitle = None
    if entity_type == "album":
        subtitle = db.scalar(select(Artist.name).where(Artist.id == entity.artist_id))
    elif entity_type == "track":
        subtitle = db.scalar(select(Artist.name).where(Artist.id == entity.artist_id))

    title = entity.name if entity_type == "artist" else entity.title
    return _submission(
        entity.id,
        entity_type,
        title,
        entity.status,
        subtitle,
        entity.rejection_reason,
    )


def set_user_admin(db: Session, username: str, make_admin: bool) -> Profile:
    profile = db.scalar(select(Profile).where(Profile.username == username.strip()))
    if profile is None:
        raise EntityNotFoundError("Usuario no encontrado")
    profile.is_admin = make_admin
    db.commit()
    db.refresh(profile)
    return profile
