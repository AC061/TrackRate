from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.schemas.social import ProfileStatsResponse, UserRatingStatsResponse
from app.services import stats_service

router = APIRouter(tags=["users"])


@router.get("/users/{user_id}/stats", response_model=ProfileStatsResponse)
def user_stats(
    user_id: UUID,
    db: Session = Depends(get_db),
) -> ProfileStatsResponse:
    try:
        return stats_service.get_profile_stats(db, user_id)
    except stats_service.StatsNotFoundError as exc:
        raise HTTPException(404, str(exc)) from exc


@router.get("/users/{user_id}/rating-stats", response_model=UserRatingStatsResponse | None)
def user_rating_stats(
    user_id: UUID,
    db: Session = Depends(get_db),
) -> UserRatingStatsResponse | None:
    return stats_service.get_user_rating_stats(db, user_id)
