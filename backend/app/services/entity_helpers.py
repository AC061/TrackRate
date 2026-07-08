from uuid import UUID

from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from app.models import Album, Artist, ModerationStatus, MusicEntityType, Track


def is_entity_approved(db: Session, entity_type: MusicEntityType, entity_id: UUID) -> bool:
    if entity_type == MusicEntityType.ARTIST:
        artist = db.get(Artist, entity_id)
        return artist is not None and artist.status == ModerationStatus.APPROVED

    if entity_type == MusicEntityType.ALBUM:
        row = db.execute(
            select(Album, Artist.status)
            .join(Artist, Artist.id == Album.artist_id)
            .where(Album.id == entity_id)
        ).first()
        if row is None:
            return False
        album, artist_status = row
        return album.status == ModerationStatus.APPROVED and artist_status == ModerationStatus.APPROVED

    if entity_type == MusicEntityType.TRACK:
        row = db.execute(
            select(Track, Artist.status, Album.status)
            .join(Artist, Artist.id == Track.artist_id)
            .outerjoin(Album, Album.id == Track.album_id)
            .where(Track.id == entity_id)
        ).first()
        if row is None:
            return False
        track, artist_status, album_status = row
        if track.status != ModerationStatus.APPROVED or artist_status != ModerationStatus.APPROVED:
            return False
        if track.album_id is not None and album_status != ModerationStatus.APPROVED:
            return False
        return True

    return False


def resolve_entity_titles(
    db: Session,
    entity_type: MusicEntityType,
    entity_id: UUID,
) -> tuple[str | None, str | None]:
    if entity_type == MusicEntityType.ARTIST:
        name = db.scalar(select(Artist.name).where(Artist.id == entity_id))
        return name, None

    if entity_type == MusicEntityType.ALBUM:
        row = db.execute(
            select(Album.title, Artist.name)
            .join(Artist, Artist.id == Album.artist_id)
            .where(Album.id == entity_id)
        ).first()
        if row is None:
            return None, None
        title, artist_name = row
        return title, artist_name

    if entity_type == MusicEntityType.TRACK:
        row = db.execute(
            select(Track.title, Artist.name)
            .join(Artist, Artist.id == Track.artist_id)
            .where(Track.id == entity_id)
        ).first()
        if row is None:
            return None, None
        title, artist_name = row
        return title, artist_name

    return None, None
