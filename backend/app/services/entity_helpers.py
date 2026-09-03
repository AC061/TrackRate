from uuid import UUID

from app.models import ListItem, MusicEntityType, Rating
from app.services.musicbrainz_client import (
    MusicBrainzClient,
    MusicBrainzError,
    MusicBrainzNotFoundError,
    artist_credit,
)

_mb = MusicBrainzClient()


def entity_exists(entity_type: MusicEntityType, entity_id: UUID) -> bool:
    return _mb.exists(entity_type.value, entity_id)


def is_entity_approved(entity_type: MusicEntityType, entity_id: UUID) -> bool:
    """Compatibilidad: entidades MusicBrainz no requieren moderación."""
    return entity_exists(entity_type, entity_id)


def fetch_entity_snapshots(
    entity_type: MusicEntityType,
    entity_id: UUID,
) -> tuple[str | None, str | None, str | None]:
    """Título, subtítulo e imagen desde MusicBrainz (sin Cover Art Archive)."""
    try:
        data = _mb.lookup(entity_type.value, entity_id)
    except (MusicBrainzNotFoundError, MusicBrainzError):
        return None, None, None

    if entity_type == MusicEntityType.ARTIST:
        return data.get("name"), data.get("disambiguation"), None

    title = data.get("title")
    subtitle = artist_credit(data)
    return title, subtitle, None


def resolve_entity_titles(
    entity_type: MusicEntityType,
    entity_id: UUID,
    *,
    stored_title: str | None = None,
    stored_subtitle: str | None = None,
) -> tuple[str | None, str | None]:
    if stored_title:
        return stored_title, stored_subtitle

    title, subtitle, _ = fetch_entity_snapshots(entity_type, entity_id)
    return title, subtitle


def resolve_rating_titles(rating: Rating) -> tuple[str | None, str | None]:
    return resolve_entity_titles(
        rating.entity_type,
        rating.entity_id,
        stored_title=rating.entity_title,
        stored_subtitle=rating.entity_subtitle,
    )


def resolve_list_item_titles(item: ListItem) -> tuple[str | None, str | None]:
    return resolve_entity_titles(
        item.entity_type,
        item.entity_id,
        stored_title=item.entity_title,
        stored_subtitle=item.entity_subtitle,
    )
