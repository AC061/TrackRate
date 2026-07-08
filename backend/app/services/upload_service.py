from uuid import UUID

from sqlalchemy.orm import Session

from app.models import Album, Artist, MusicList, Profile, Track
from app.services.storage_service import (
    BUCKET_ARTIST_IMAGES,
    BUCKET_AVATARS,
    BUCKET_CATALOG_COVERS,
    BUCKET_LIST_COVERS,
    StorageError,
    delete_public_url,
    upload_bytes,
    validate_image_upload,
)


class UploadError(Exception):
    pass


class UploadNotFoundError(Exception):
    pass


class UploadAccessError(Exception):
    pass


def _save_image(
    bucket: str,
    object_key: str,
    data: bytes,
    content_type: str | None,
    previous_url: str | None,
) -> str:
    try:
        extension = validate_image_upload(bucket, content_type, len(data))
    except StorageError as exc:
        raise UploadError(str(exc)) from exc

    if not object_key.endswith(f".{extension}"):
        object_key = f"{object_key.rsplit('.', 1)[0]}.{extension}"

    url = upload_bytes(bucket, object_key, data, content_type or "application/octet-stream")
    if previous_url and previous_url != url:
        delete_public_url(previous_url)
    return url


def upload_avatar(
    db: Session,
    user_id: UUID,
    data: bytes,
    content_type: str | None,
) -> str:
    profile = db.get(Profile, user_id)
    if profile is None:
        raise UploadNotFoundError("Perfil no encontrado")

    object_key = f"{user_id}/avatar"
    url = _save_image(BUCKET_AVATARS, object_key, data, content_type, profile.avatar_url)
    profile.avatar_url = url
    db.commit()
    db.refresh(profile)
    return url


def upload_artist_image(
    db: Session,
    user_id: UUID,
    artist_id: UUID,
    data: bytes,
    content_type: str | None,
) -> str:
    artist = db.get(Artist, artist_id)
    if artist is None:
        raise UploadNotFoundError("Artista no encontrado")
    if artist.submitted_by != user_id:
        raise UploadAccessError("Solo el autor del envío puede subir la imagen")

    object_key = f"{user_id}/{artist_id}"
    url = _save_image(
        BUCKET_ARTIST_IMAGES,
        object_key,
        data,
        content_type,
        artist.image_url,
    )
    artist.image_url = url
    db.commit()
    db.refresh(artist)
    return url


def upload_catalog_cover(
    db: Session,
    user_id: UUID,
    entity_type: str,
    entity_id: UUID,
    data: bytes,
    content_type: str | None,
) -> str:
    if entity_type == "album":
        album = db.get(Album, entity_id)
        if album is None:
            raise UploadNotFoundError("Álbum no encontrado")
        if album.submitted_by != user_id:
            raise UploadAccessError("Solo el autor del envío puede subir la portada")

        object_key = f"{user_id}/album/{entity_id}"
        url = _save_image(
            BUCKET_CATALOG_COVERS,
            object_key,
            data,
            content_type,
            album.cover_url,
        )
        album.cover_url = url
        db.commit()
        db.refresh(album)
        return url

    if entity_type == "track":
        track = db.get(Track, entity_id)
        if track is None:
            raise UploadNotFoundError("Canción no encontrada")
        if track.submitted_by != user_id:
            raise UploadAccessError("Solo el autor del envío puede subir la portada")

        object_key = f"{user_id}/track/{entity_id}"
        url = _save_image(
            BUCKET_CATALOG_COVERS,
            object_key,
            data,
            content_type,
            track.cover_url,
        )
        track.cover_url = url
        db.commit()
        db.refresh(track)
        return url

    raise UploadError("Tipo de entidad no válido para portada")


def upload_list_cover(
    db: Session,
    user_id: UUID,
    list_id: UUID,
    data: bytes,
    content_type: str | None,
) -> str:
    music_list = db.get(MusicList, list_id)
    if music_list is None:
        raise UploadNotFoundError("Lista no encontrada")
    if music_list.user_id != user_id:
        raise UploadAccessError("No tienes permiso para editar esta lista")

    object_key = f"{user_id}/{list_id}"
    url = _save_image(
        BUCKET_LIST_COVERS,
        object_key,
        data,
        content_type,
        music_list.cover_url,
    )
    music_list.cover_url = url
    db.commit()
    db.refresh(music_list)
    return url
