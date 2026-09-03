from uuid import UUID

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from app.core.security import decode_access_token
from app.db.session import get_db
from app.models import User
from app.services.auth_service import get_user_by_id
from app.services.profile_service import load_user_with_profile

bearer_scheme = HTTPBearer(auto_error=False)


def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    db: Session = Depends(get_db),
) -> User:
    if credentials is None or credentials.scheme.lower() != "bearer":
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "No autenticado")
    try:
        user_id = decode_access_token(credentials.credentials)
    except ValueError as exc:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Token inválido") from exc

    user = get_user_by_id(db, user_id)
    if user is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Usuario no encontrado")
    return user


def get_current_user_with_profile(
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> User:
    loaded = load_user_with_profile(db, user.id)
    if loaded is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Usuario no encontrado")
    return loaded


def require_admin(user: User = Depends(get_current_user_with_profile)) -> User:
    if user.profile is None or not user.profile.is_admin:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "Se requieren permisos de administrador")
    return user


def get_optional_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    db: Session = Depends(get_db),
) -> User | None:
    if credentials is None or credentials.scheme.lower() != "bearer":
        return None
    try:
        user_id = decode_access_token(credentials.credentials)
    except ValueError:
        return None
    return get_user_by_id(db, user_id)
