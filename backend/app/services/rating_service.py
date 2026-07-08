from datetime import date
from decimal import Decimal
from uuid import UUID

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models import Activity, ActivityType, MusicEntityType, Rating
from app.schemas.social import RatingDetailResponse, RatingResponse, RatingStatsResponse
from app.services.entity_helpers import is_entity_approved, resolve_entity_titles


class RatingError(Exception):
    pass


class RatingNotFoundError(Exception):
    pass


def _parse_entity_type(raw: str) -> MusicEntityType:
    return MusicEntityType(raw)


def _to_rating_response(rating: Rating) -> RatingResponse:
    score = float(rating.rating) if isinstance(rating.rating, Decimal) else rating.rating
    return RatingResponse(
        id=rating.id,
        rating=score,
        review=rating.review,
        listened_at=rating.listened_at,
    )


def _activity_type_for_rating(rating: Rating, is_update: bool) -> ActivityType:
    if is_update:
        return ActivityType.UPDATED
    if rating.review and rating.review.strip():
        return ActivityType.REVIEWED
    return ActivityType.RATED


def _record_activity(db: Session, rating: Rating, is_update: bool) -> None:
    db.add(
        Activity(
            user_id=rating.user_id,
            rating_id=rating.id,
            activity_type=_activity_type_for_rating(rating, is_update),
        )
    )


def get_entity_stats(
    db: Session,
    entity_type: str,
    entity_id: UUID,
) -> RatingStatsResponse | None:
    parsed_type = _parse_entity_type(entity_type)
    row = db.execute(
        select(func.round(func.avg(Rating.rating), 2), func.count())
        .where(
            Rating.entity_type == parsed_type,
            Rating.entity_id == entity_id,
        )
    ).one()
    average, count = row
    if not count:
        return None
    return RatingStatsResponse(average=float(average), count=int(count))


def get_user_rating(
    db: Session,
    user_id: UUID,
    entity_type: str,
    entity_id: UUID,
) -> RatingResponse | None:
    parsed_type = _parse_entity_type(entity_type)
    rating = db.scalar(
        select(Rating).where(
            Rating.user_id == user_id,
            Rating.entity_type == parsed_type,
            Rating.entity_id == entity_id,
        )
    )
    return _to_rating_response(rating) if rating else None


def upsert_rating(
    db: Session,
    user_id: UUID,
    entity_type: str,
    entity_id: UUID,
    score: float,
    review: str | None,
    listened_at: date | None,
) -> RatingResponse:
    parsed_type = _parse_entity_type(entity_type)
    if not is_entity_approved(db, parsed_type, entity_id):
        raise RatingError("Solo se pueden valorar entidades aprobadas del catálogo")

    clean_review = review.strip() if review else None
    if clean_review == "":
        clean_review = None

    existing = db.scalar(
        select(Rating).where(
            Rating.user_id == user_id,
            Rating.entity_type == parsed_type,
            Rating.entity_id == entity_id,
        )
    )

    if existing:
        existing.rating = score
        existing.review = clean_review
        existing.listened_at = listened_at
        db.flush()
        _record_activity(db, existing, is_update=True)
        db.commit()
        db.refresh(existing)
        return _to_rating_response(existing)

    rating = Rating(
        user_id=user_id,
        entity_type=parsed_type,
        entity_id=entity_id,
        rating=score,
        review=clean_review,
        listened_at=listened_at,
    )
    db.add(rating)
    db.flush()
    _record_activity(db, rating, is_update=False)
    db.commit()
    db.refresh(rating)
    return _to_rating_response(rating)


def delete_rating(
    db: Session,
    user_id: UUID,
    entity_type: str,
    entity_id: UUID,
) -> None:
    parsed_type = _parse_entity_type(entity_type)
    rating = db.scalar(
        select(Rating).where(
            Rating.user_id == user_id,
            Rating.entity_type == parsed_type,
            Rating.entity_id == entity_id,
        )
    )
    if rating is None:
        raise RatingNotFoundError("Valoración no encontrada")
    db.delete(rating)
    db.commit()


def get_user_diary(db: Session, user_id: UUID) -> list[RatingDetailResponse]:
    ratings = db.scalars(
        select(Rating)
        .where(Rating.user_id == user_id)
        .order_by(Rating.created_at.desc())
    )
    entries: list[RatingDetailResponse] = []
    for rating in ratings:
        title, subtitle = resolve_entity_titles(db, rating.entity_type, rating.entity_id)
        score = float(rating.rating) if isinstance(rating.rating, Decimal) else rating.rating
        entries.append(
            RatingDetailResponse(
                id=rating.id,
                entity_type=rating.entity_type.value,
                entity_id=rating.entity_id,
                rating=score,
                review=rating.review,
                listened_at=rating.listened_at,
                entity_title=title,
                entity_subtitle=subtitle,
            )
        )
    return entries
