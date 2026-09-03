from uuid import UUID

from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models import Follow, Profile


class FollowError(Exception):
    pass


class UserNotFoundError(Exception):
    pass


def get_following_ids(db: Session, user_id: UUID) -> list[UUID]:
    return list(
        db.scalars(
            select(Follow.following_id)
            .where(Follow.follower_id == user_id)
            .order_by(Follow.created_at.desc())
        )
    )


def follow_user(db: Session, follower_id: UUID, following_id: UUID) -> None:
    if follower_id == following_id:
        raise FollowError("No puedes seguirte a ti mismo")

    target = db.get(Profile, following_id)
    if target is None:
        raise UserNotFoundError("Usuario no encontrado")

    follow = Follow(follower_id=follower_id, following_id=following_id)
    db.add(follow)
    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise FollowError("Ya sigues a este usuario") from exc


def unfollow_user(db: Session, follower_id: UUID, following_id: UUID) -> None:
    follow = db.get(Follow, {"follower_id": follower_id, "following_id": following_id})
    if follow is None:
        return
    db.delete(follow)
    db.commit()


def is_following(db: Session, follower_id: UUID, following_id: UUID) -> bool:
    follow = db.get(Follow, {"follower_id": follower_id, "following_id": following_id})
    return follow is not None
