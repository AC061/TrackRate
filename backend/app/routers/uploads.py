from uuid import UUID

from fastapi import APIRouter, Depends, File, HTTPException, UploadFile
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.deps.auth import get_current_user_with_profile
from app.models import User
from app.schemas.uploads import UploadResponse
from app.services import upload_service

router = APIRouter(tags=["uploads"])

MAX_READ_BYTES = 10 * 1024 * 1024 + 1


async def _read_upload(file: UploadFile) -> tuple[bytes, str | None]:
    data = await file.read(MAX_READ_BYTES)
    return data, file.content_type


@router.post("/me/avatar", response_model=UploadResponse)
async def upload_avatar(
    file: UploadFile = File(...),
    user: User = Depends(get_current_user_with_profile),
    db: Session = Depends(get_db),
) -> UploadResponse:
    data, content_type = await _read_upload(file)
    try:
        url = upload_service.upload_avatar(db, user.id, data, content_type)
    except upload_service.UploadNotFoundError as exc:
        raise HTTPException(404, str(exc)) from exc
    except upload_service.UploadError as exc:
        raise HTTPException(400, str(exc)) from exc
    return UploadResponse(url=url)


@router.post("/catalog/artists/{artist_id}/image", response_model=UploadResponse)
async def upload_artist_image(
    artist_id: UUID,
    file: UploadFile = File(...),
    user: User = Depends(get_current_user_with_profile),
    db: Session = Depends(get_db),
) -> UploadResponse:
    data, content_type = await _read_upload(file)
    try:
        url = upload_service.upload_artist_image(db, user.id, artist_id, data, content_type)
    except upload_service.UploadNotFoundError as exc:
        raise HTTPException(404, str(exc)) from exc
    except upload_service.UploadAccessError as exc:
        raise HTTPException(403, str(exc)) from exc
    except upload_service.UploadError as exc:
        raise HTTPException(400, str(exc)) from exc
    return UploadResponse(url=url)


@router.post("/catalog/albums/{album_id}/cover", response_model=UploadResponse)
async def upload_album_cover(
    album_id: UUID,
    file: UploadFile = File(...),
    user: User = Depends(get_current_user_with_profile),
    db: Session = Depends(get_db),
) -> UploadResponse:
    data, content_type = await _read_upload(file)
    try:
        url = upload_service.upload_catalog_cover(
            db, user.id, "album", album_id, data, content_type
        )
    except upload_service.UploadNotFoundError as exc:
        raise HTTPException(404, str(exc)) from exc
    except upload_service.UploadAccessError as exc:
        raise HTTPException(403, str(exc)) from exc
    except upload_service.UploadError as exc:
        raise HTTPException(400, str(exc)) from exc
    return UploadResponse(url=url)


@router.post("/catalog/tracks/{track_id}/cover", response_model=UploadResponse)
async def upload_track_cover(
    track_id: UUID,
    file: UploadFile = File(...),
    user: User = Depends(get_current_user_with_profile),
    db: Session = Depends(get_db),
) -> UploadResponse:
    data, content_type = await _read_upload(file)
    try:
        url = upload_service.upload_catalog_cover(
            db, user.id, "track", track_id, data, content_type
        )
    except upload_service.UploadNotFoundError as exc:
        raise HTTPException(404, str(exc)) from exc
    except upload_service.UploadAccessError as exc:
        raise HTTPException(403, str(exc)) from exc
    except upload_service.UploadError as exc:
        raise HTTPException(400, str(exc)) from exc
    return UploadResponse(url=url)


@router.post("/me/lists/{list_id}/cover", response_model=UploadResponse)
async def upload_list_cover(
    list_id: UUID,
    file: UploadFile = File(...),
    user: User = Depends(get_current_user_with_profile),
    db: Session = Depends(get_db),
) -> UploadResponse:
    data, content_type = await _read_upload(file)
    try:
        url = upload_service.upload_list_cover(db, user.id, list_id, data, content_type)
    except upload_service.UploadNotFoundError as exc:
        raise HTTPException(404, str(exc)) from exc
    except upload_service.UploadAccessError as exc:
        raise HTTPException(403, str(exc)) from exc
    except upload_service.UploadError as exc:
        raise HTTPException(400, str(exc)) from exc
    return UploadResponse(url=url)
