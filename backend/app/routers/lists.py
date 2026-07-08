from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, Response
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.deps.auth import get_current_user, get_optional_user
from app.models import User
from app.schemas.social import AddListItemRequest, CreateListRequest, ListItemResponse, MusicListResponse
from app.services import list_service

router = APIRouter(tags=["lists"])


@router.get("/me/lists", response_model=list[MusicListResponse])
def get_my_lists(
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> list[MusicListResponse]:
    return list_service.get_my_lists(db, user.id)


@router.post("/me/lists", response_model=MusicListResponse, status_code=201)
def create_list(
    body: CreateListRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> MusicListResponse:
    return list_service.create_list(
        db, user.id, body.title, body.description, body.is_public
    )


@router.delete("/me/lists/{list_id}", status_code=204, response_class=Response)
def delete_list(
    list_id: UUID,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> Response:
    try:
        list_service.delete_list(db, user.id, list_id)
    except list_service.ListNotFoundError as exc:
        raise HTTPException(404, str(exc)) from exc
    except list_service.ListAccessError as exc:
        raise HTTPException(403, str(exc)) from exc
    return Response(status_code=204)


@router.get("/lists/{list_id}/items", response_model=list[ListItemResponse])
def get_list_items(
    list_id: UUID,
    user: User | None = Depends(get_optional_user),
    db: Session = Depends(get_db),
) -> list[ListItemResponse]:
    viewer_id = user.id if user else None
    try:
        return list_service.get_list_items(db, list_id, viewer_id)
    except list_service.ListNotFoundError as exc:
        raise HTTPException(404, str(exc)) from exc
    except list_service.ListAccessError as exc:
        raise HTTPException(403, str(exc)) from exc


@router.post("/me/lists/{list_id}/items", response_model=ListItemResponse, status_code=201)
def add_list_item(
    list_id: UUID,
    body: AddListItemRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> ListItemResponse:
    try:
        return list_service.add_list_item(
            db, user.id, list_id, body.entity_type, body.entity_id
        )
    except list_service.ListNotFoundError as exc:
        raise HTTPException(404, str(exc)) from exc
    except list_service.ListAccessError as exc:
        raise HTTPException(403, str(exc)) from exc
    except list_service.ListError as exc:
        raise HTTPException(400, str(exc)) from exc


@router.delete("/me/lists/{list_id}/items", status_code=204, response_class=Response)
def remove_list_item(
    list_id: UUID,
    entity_type: str = Query(pattern=r"^(artist|album|track)$"),
    entity_id: UUID = Query(),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> Response:
    try:
        list_service.remove_list_item(db, user.id, list_id, entity_type, entity_id)
    except list_service.ListNotFoundError as exc:
        raise HTTPException(404, str(exc)) from exc
    except list_service.ListAccessError as exc:
        raise HTTPException(403, str(exc)) from exc
    return Response(status_code=204)
