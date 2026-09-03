from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.deps.auth import get_current_user
from app.models import User
from app.schemas.social import ActivityFeedResponse
from app.services import feed_service

router = APIRouter(tags=["feed"])


@router.get("/feed", response_model=list[ActivityFeedResponse])
def get_feed(
    limit: int = Query(default=50, ge=1, le=50),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> list[ActivityFeedResponse]:
    return feed_service.get_feed(db, user.id, limit)
