from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.core.security import create_access_token
from app.db.session import get_db
from app.deps.auth import get_current_user_with_profile
from app.models import User
from app.schemas.auth import LoginRequest, ProfileResponse, RegisterRequest, TokenResponse, UserResponse
from app.schemas.mappers import to_profile_response, to_token_response, to_user_response
from app.services.auth_service import (
    EmailAlreadyRegisteredError,
    InvalidCredentialsError,
    authenticate_user,
    register_user,
)
from app.services.profile_service import load_user_with_profile

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/register", response_model=TokenResponse, status_code=status.HTTP_201_CREATED)
def register(body: RegisterRequest, db: Session = Depends(get_db)) -> TokenResponse:
    try:
        user = register_user(db, body.email, body.password)
    except EmailAlreadyRegisteredError as exc:
        raise HTTPException(status.HTTP_409_CONFLICT, str(exc)) from exc

    loaded = load_user_with_profile(db, user.id)
    if loaded is None:
        raise HTTPException(status.HTTP_500_INTERNAL_SERVER_ERROR, "Error al crear perfil")
    token = create_access_token(loaded.id)
    return to_token_response(loaded, token)


@router.post("/login", response_model=TokenResponse)
def login(body: LoginRequest, db: Session = Depends(get_db)) -> TokenResponse:
    try:
        user = authenticate_user(db, body.email, body.password)
    except InvalidCredentialsError as exc:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, str(exc)) from exc

    loaded = load_user_with_profile(db, user.id)
    if loaded is None:
        raise HTTPException(status.HTTP_500_INTERNAL_SERVER_ERROR, "Perfil no encontrado")
    token = create_access_token(loaded.id)
    return to_token_response(loaded, token)


@router.get("/me", response_model=UserResponse)
def me(user: User = Depends(get_current_user_with_profile)) -> UserResponse:
    return to_user_response(user)


@router.get("/me/profile", response_model=ProfileResponse)
def me_profile(user: User = Depends(get_current_user_with_profile)) -> ProfileResponse:
    return to_profile_response(user.profile)
