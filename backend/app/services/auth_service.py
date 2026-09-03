import re
from uuid import UUID, uuid4

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.security import hash_password, verify_password
from app.models import Profile, User


class AuthError(Exception):
    pass


class EmailAlreadyRegisteredError(AuthError):
    pass


class InvalidCredentialsError(AuthError):
    pass


def _base_username_from_email(email: str) -> str:
    local = email.split("@", 1)[0].lower()
    cleaned = re.sub(r"[^a-z0-9_]", "_", local)[:24]
    if len(cleaned) < 3:
        cleaned = "user"
    return cleaned


def _unique_username(db: Session, base: str) -> str:
    candidate = base[:30]
    suffix = 0
    while db.scalar(select(Profile.id).where(Profile.username == candidate)):
        suffix += 1
        prefix = base[: max(3, 30 - len(str(suffix)))]
        candidate = f"{prefix}{suffix}"
    return candidate


def register_user(db: Session, email: str, password: str) -> User:
    normalized_email = email.strip().lower()
    existing = db.scalar(select(User).where(User.email == normalized_email))
    if existing:
        raise EmailAlreadyRegisteredError("Este email ya está registrado")

    user_id = uuid4()
    user = User(
        id=user_id,
        email=normalized_email,
        password_hash=hash_password(password),
    )
    username = _unique_username(db, _base_username_from_email(normalized_email))
    profile = Profile(
        id=user_id,
        username=username,
        first_name="",
        last_name="",
        display_name=username,
    )
    db.add(user)
    db.add(profile)
    db.commit()
    db.refresh(user)
    return user


def authenticate_user(db: Session, identifier: str, password: str) -> User:
    normalized = identifier.strip()
    if "@" in normalized:
        user = db.scalar(select(User).where(User.email == normalized.lower()))
    else:
        profile = db.scalar(select(Profile).where(Profile.username == normalized.lower()))
        user = db.get(User, profile.id) if profile else None

    if user is None or not verify_password(password, user.password_hash):
        raise InvalidCredentialsError("Usuario o correo y contraseña incorrectos")
    return user


def change_password(db: Session, user_id: UUID, current_password: str, new_password: str) -> None:
    user = db.get(User, user_id)
    if user is None:
        raise InvalidCredentialsError("Usuario no encontrado")
    if not verify_password(current_password, user.password_hash):
        raise InvalidCredentialsError("Contraseña actual incorrecta")
    user.password_hash = hash_password(new_password)
    db.commit()


def get_user_by_id(db: Session, user_id: UUID) -> User | None:
    return db.get(User, user_id)
