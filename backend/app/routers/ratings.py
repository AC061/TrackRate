from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, Response
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.deps.auth import get_current_user
from app.models import User
from app.schemas.social import (
    RatingDetailResponse,
    RatingResponse,
    RatingUpsertRequest,
)
from app.services import rating_service

router = APIRouter(tags=["ratings"])


@router.get("/me/ratings", response_model=RatingResponse | None)
def get_my_rating(
    entity_type: str = Query(pattern=r"^(artist|album|track)$"),
    entity_id: UUID = Query(),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> RatingResponse | None:
    return rating_service.get_user_rating(db, user.id, entity_type, entity_id)


@router.put("/me/ratings", response_model=RatingResponse)
def upsert_my_rating(
    body: RatingUpsertRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> RatingResponse:
    try:
        return rating_service.upsert_rating(
            db,
            user.id,
            body.entity_type,
            body.entity_id,
            body.rating,
            body.review,
            body.listened_at,
        )
    except rating_service.RatingError as exc:
        raise HTTPException(400, str(exc)) from exc


@router.delete("/me/ratings", status_code=204, response_class=Response)
def delete_my_rating(
    entity_type: str = Query(pattern=r"^(artist|album|track)$"),
    entity_id: UUID = Query(),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> Response:
    try:
        rating_service.delete_rating(db, user.id, entity_type, entity_id)
    except rating_service.RatingNotFoundError as exc:
        raise HTTPException(404, str(exc)) from exc
    return Response(status_code=204)


@router.get("/me/diary", response_model=list[RatingDetailResponse])
def get_my_diary(
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> list[RatingDetailResponse]:
    return rating_service.get_user_diary(db, user.id)


@router.get("/users/{user_id}/diary", response_model=list[RatingDetailResponse])
def get_user_diary(
    user_id: UUID,
    db: Session = Depends(get_db),
) -> list[RatingDetailResponse]:
    return rating_service.get_user_diary(db, user_id)
