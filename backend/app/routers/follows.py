from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, Response
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.deps.auth import get_current_user
from app.models import User
from app.schemas.social import FollowCheckResponse, FollowingIdResponse
from app.services import follow_service

router = APIRouter(tags=["follows"])


@router.get("/me/following", response_model=list[FollowingIdResponse])
def list_following(
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> list[FollowingIdResponse]:
    ids = follow_service.get_following_ids(db, user.id)
    return [FollowingIdResponse(following_id=fid) for fid in ids]


@router.post("/me/following/{following_id}", status_code=204, response_class=Response)
def follow_user(
    following_id: UUID,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> Response:
    try:
        follow_service.follow_user(db, user.id, following_id)
    except follow_service.UserNotFoundError as exc:
        raise HTTPException(404, str(exc)) from exc
    except follow_service.FollowError as exc:
        raise HTTPException(400, str(exc)) from exc
    return Response(status_code=204)


@router.delete("/me/following/{following_id}", status_code=204, response_class=Response)
def unfollow_user(
    following_id: UUID,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> Response:
    follow_service.unfollow_user(db, user.id, following_id)
    return Response(status_code=204)


@router.get("/me/following/{following_id}", response_model=list[FollowCheckResponse])
def check_following(
    following_id: UUID,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> list[FollowCheckResponse]:
    if follow_service.is_following(db, user.id, following_id):
        return [FollowCheckResponse(follower_id=user.id)]
    return []
