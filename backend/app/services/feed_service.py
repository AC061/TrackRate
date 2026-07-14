from uuid import UUID

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models import Activity, Profile, Rating
from app.schemas.social import ActivityFeedResponse
from app.services.entity_helpers import resolve_entity_titles
from app.services.follow_service import get_following_ids
from app.services.storage_service import normalize_stored_media_url

FEED_LIMIT = 50


def get_feed(db: Session, user_id: UUID, limit: int = FEED_LIMIT) -> list[ActivityFeedResponse]:
    following_ids = get_following_ids(db, user_id)
    feed_user_ids = list(dict.fromkeys([*following_ids, user_id]))

    rows = db.execute(
        select(Activity, Profile, Rating)
        .join(Profile, Profile.id == Activity.user_id)
        .join(Rating, Rating.id == Activity.rating_id)
        .where(Activity.user_id.in_(feed_user_ids))
        .order_by(Activity.created_at.desc())
        .limit(min(limit, FEED_LIMIT))
    )

    items: list[ActivityFeedResponse] = []
    for activity, profile, rating in rows:
        title, subtitle = resolve_entity_titles(db, rating.entity_type, rating.entity_id)
        score = float(rating.rating)
        items.append(
            ActivityFeedResponse(
                id=activity.id,
                user_id=activity.user_id,
                username=profile.username,
                display_name=profile.display_name,
                avatar_url=normalize_stored_media_url(profile.avatar_url),
                activity_type=activity.activity_type.value,
                created_at=activity.created_at,
                rating=score,
                review=rating.review,
                entity_type=rating.entity_type.value,
                entity_id=rating.entity_id,
                entity_title=title,
                entity_subtitle=subtitle,
            )
        )
    return items
