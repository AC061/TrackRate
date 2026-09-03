from uuid import UUID

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models import Follow, Profile, Rating
from app.schemas.social import ProfileStatsResponse, UserRatingStatsResponse


class StatsNotFoundError(Exception):
    pass


def get_profile_stats(db: Session, user_id: UUID) -> ProfileStatsResponse:
    profile = db.get(Profile, user_id)
    if profile is None:
        raise StatsNotFoundError("Usuario no encontrado")

    follower_count = db.scalar(
        select(func.count()).select_from(Follow).where(Follow.following_id == user_id)
    )
    following_count = db.scalar(
        select(func.count()).select_from(Follow).where(Follow.follower_id == user_id)
    )
    rating_count = db.scalar(
        select(func.count()).select_from(Rating).where(Rating.user_id == user_id)
    )

    return ProfileStatsResponse(
        follower_count=int(follower_count or 0),
        following_count=int(following_count or 0),
        rating_count=int(rating_count or 0),
    )


def get_user_rating_stats(db: Session, user_id: UUID) -> UserRatingStatsResponse | None:
    row = db.execute(
        select(
            func.count(),
            func.round(func.avg(Rating.rating), 2),
            func.count().filter(
                Rating.review.is_not(None),
                func.length(func.trim(Rating.review)) > 0,
            ),
        ).where(Rating.user_id == user_id)
    ).one()

    total, average, review_count = row
    if not total:
        return None

    return UserRatingStatsResponse(
        total_ratings=int(total),
        average_rating=float(average),
        review_count=int(review_count or 0),
    )
