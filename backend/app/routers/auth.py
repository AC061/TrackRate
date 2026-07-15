from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.core.security import create_access_token
from app.db.session import get_db
from app.deps.auth import get_current_user_with_profile
from app.models import User
from app.schemas.auth import (
    ChangePasswordRequest,
    LoginRequest,
    MessageResponse,
    ProfileResponse,
    RegisterRequest,
    TokenResponse,
    UserResponse,
)
from app.schemas.mappers import (
    to_profile_response,
    to_token_response,
    to_user_response,
)
from app.services.auth_service import (
    CurrentPasswordIncorrectError,
    EmailAlreadyRegisteredError,
    InvalidCredentialsError,
    NewPasswordSameAsCurrentError,
    authenticate_user,
    change_user_password,
    register_user,
)
from app.services.profile_service import load_user_with_profile

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post(
    "/register",
    response_model=TokenResponse,
    status_code=status.HTTP_201_CREATED,
)
def register(
    body: RegisterRequest,
    db: Session = Depends(get_db),
) -> TokenResponse:
    try:
        user = register_user(
            db,
            body.email,
            body.password,
        )
    except EmailAlreadyRegisteredError as exc:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=str(exc),
        ) from exc

    loaded = load_user_with_profile(db, user.id)

    if loaded is None:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error al crear perfil",
        )

    token = create_access_token(loaded.id)

    return to_token_response(
        loaded,
        token,
    )


@router.post(
    "/login",
    response_model=TokenResponse,
)
def login(
    body: LoginRequest,
    db: Session = Depends(get_db),
) -> TokenResponse:
    try:
        user = authenticate_user(
            db,
            body.email,
            body.password,
        )
    except InvalidCredentialsError as exc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=str(exc),
        ) from exc

    loaded = load_user_with_profile(db, user.id)

    if loaded is None:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Perfil no encontrado",
        )

    token = create_access_token(loaded.id)

    return to_token_response(
        loaded,
        token,
    )


@router.get(
    "/me",
    response_model=UserResponse,
)
def me(
    user: User = Depends(get_current_user_with_profile),
) -> UserResponse:
    return to_user_response(user)


@router.get(
    "/me/profile",
    response_model=ProfileResponse,
)
def me_profile(
    user: User = Depends(get_current_user_with_profile),
) -> ProfileResponse:
    return to_profile_response(user.profile)


@router.post(
    "/change-password",
    response_model=MessageResponse,
)
def change_password(
    body: ChangePasswordRequest,
    user: User = Depends(get_current_user_with_profile),
    db: Session = Depends(get_db),
) -> MessageResponse:
    try:
        change_user_password(
            db=db,
            user=user,
            current_password=body.current_password,
            new_password=body.new_password,
        )
    except CurrentPasswordIncorrectError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        ) from exc
    except NewPasswordSameAsCurrentError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        ) from exc

    return MessageResponse(
        detail="Contraseña actualizada correctamente"
    )
