from uuid import UUID

from sqlalchemy import select
from sqlalchemy.orm import Session, joinedload

from app.models import Profile, User


class ProfileNotFoundError(Exception):
    pass


class UsernameTakenError(Exception):
    pass


class ProfileValidationError(Exception):
    pass


def get_profile_by_username(db: Session, username: str) -> Profile:
    profile = db.scalar(select(Profile).where(Profile.username == username.strip()))
    if profile is None:
        raise ProfileNotFoundError("Usuario no encontrado")
    return profile


def get_profile_by_id(db: Session, user_id: UUID) -> Profile | None:
    return db.get(Profile, user_id)


def update_profile(
    db: Session,
    user_id: UUID,
    username: str,
    first_name: str,
    last_name: str,
    display_name: str | None,
    bio: str | None,
) -> Profile:
    profile = db.get(Profile, user_id)
    if profile is None:
        raise ProfileNotFoundError("Perfil no encontrado")

    normalized_first_name = first_name.strip()
    normalized_last_name = last_name.strip()
    if not normalized_first_name:
        raise ProfileValidationError("El nombre es obligatorio")
    if not normalized_last_name:
        raise ProfileValidationError("El apellido es obligatorio")

    normalized_username = username.strip()
    if normalized_username != profile.username:
        taken = db.scalar(
            select(Profile.id).where(
                Profile.username == normalized_username,
                Profile.id != user_id,
            )
        )
        if taken:
            raise UsernameTakenError("Ese nombre de usuario ya está en uso")
        profile.username = normalized_username

    profile.first_name = normalized_first_name
    profile.last_name = normalized_last_name
    profile.display_name = display_name or f"{normalized_first_name} {normalized_last_name}".strip()
    profile.bio = bio
    db.commit()
    db.refresh(profile)
    return profile


def load_user_with_profile(db: Session, user_id: UUID) -> User | None:
    return db.scalar(
        select(User).options(joinedload(User.profile)).where(User.id == user_id)
    )
