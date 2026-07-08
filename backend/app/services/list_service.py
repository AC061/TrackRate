from uuid import UUID

from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models import ListItem, MusicEntityType, MusicList
from app.schemas.social import ListItemResponse, MusicListResponse
from app.services.entity_helpers import is_entity_approved, resolve_entity_titles


class ListError(Exception):
    pass


class ListNotFoundError(Exception):
    pass


class ListAccessError(Exception):
    pass


def _parse_entity_type(raw: str) -> MusicEntityType:
    return MusicEntityType(raw)


def get_my_lists(db: Session, user_id: UUID) -> list[MusicListResponse]:
    lists = db.scalars(
        select(MusicList)
        .where(MusicList.user_id == user_id)
        .order_by(MusicList.created_at.desc())
    )
    return [MusicListResponse.model_validate(item) for item in lists]


def create_list(
    db: Session,
    user_id: UUID,
    title: str,
    description: str | None,
    is_public: bool,
) -> MusicListResponse:
    clean_description = description.strip() if description else None
    if clean_description == "":
        clean_description = None

    music_list = MusicList(
        user_id=user_id,
        title=title.strip(),
        description=clean_description,
        is_public=is_public,
    )
    db.add(music_list)
    db.commit()
    db.refresh(music_list)
    return MusicListResponse.model_validate(music_list)


def delete_list(db: Session, user_id: UUID, list_id: UUID) -> None:
    music_list = db.get(MusicList, list_id)
    if music_list is None:
        raise ListNotFoundError("Lista no encontrada")
    if music_list.user_id != user_id:
        raise ListAccessError("No tienes permiso para eliminar esta lista")
    db.delete(music_list)
    db.commit()


def _can_view_list(music_list: MusicList, viewer_id: UUID | None) -> bool:
    if music_list.is_public:
        return True
    return viewer_id is not None and music_list.user_id == viewer_id


def get_list_items(
    db: Session,
    list_id: UUID,
    viewer_id: UUID | None,
) -> list[ListItemResponse]:
    music_list = db.get(MusicList, list_id)
    if music_list is None:
        raise ListNotFoundError("Lista no encontrada")
    if not _can_view_list(music_list, viewer_id):
        raise ListAccessError("No tienes permiso para ver esta lista")

    items = db.scalars(
        select(ListItem)
        .where(ListItem.list_id == list_id)
        .order_by(ListItem.position.asc())
    )
    result: list[ListItemResponse] = []
    for item in items:
        title, subtitle = resolve_entity_titles(db, item.entity_type, item.entity_id)
        result.append(
            ListItemResponse(
                list_id=item.list_id,
                entity_type=item.entity_type.value,
                entity_id=item.entity_id,
                position=item.position,
                entity_title=title,
                entity_subtitle=subtitle,
            )
        )
    return result


def add_list_item(
    db: Session,
    user_id: UUID,
    list_id: UUID,
    entity_type: str,
    entity_id: UUID,
) -> ListItemResponse:
    music_list = db.get(MusicList, list_id)
    if music_list is None:
        raise ListNotFoundError("Lista no encontrada")
    if music_list.user_id != user_id:
        raise ListAccessError("No tienes permiso para editar esta lista")

    parsed_type = _parse_entity_type(entity_type)
    if not is_entity_approved(db, parsed_type, entity_id):
        raise ListError("Solo se pueden añadir entidades aprobadas del catálogo")

    max_position = db.scalar(
        select(func.coalesce(func.max(ListItem.position), -1)).where(ListItem.list_id == list_id)
    )
    position = (max_position or -1) + 1

    item = ListItem(
        list_id=list_id,
        entity_type=parsed_type,
        entity_id=entity_id,
        position=position,
    )
    db.add(item)
    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise ListError("Esta entidad ya está en la lista") from exc
    db.refresh(item)

    title, subtitle = resolve_entity_titles(db, parsed_type, entity_id)
    return ListItemResponse(
        list_id=item.list_id,
        entity_type=item.entity_type.value,
        entity_id=item.entity_id,
        position=item.position,
        entity_title=title,
        entity_subtitle=subtitle,
    )


def remove_list_item(
    db: Session,
    user_id: UUID,
    list_id: UUID,
    entity_type: str,
    entity_id: UUID,
) -> None:
    music_list = db.get(MusicList, list_id)
    if music_list is None:
        raise ListNotFoundError("Lista no encontrada")
    if music_list.user_id != user_id:
        raise ListAccessError("No tienes permiso para editar esta lista")

    parsed_type = _parse_entity_type(entity_type)
    item = db.get(
        ListItem,
        {"list_id": list_id, "entity_type": parsed_type, "entity_id": entity_id},
    )
    if item is None:
        return
    db.delete(item)
    db.commit()
