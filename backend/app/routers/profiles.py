from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.deps.auth import get_current_user_with_profile
from app.models import User
from app.schemas.auth import ProfileResponse, ProfileUpdateRequest
from app.schemas.mappers import to_profile_response
from app.services.profile_service import (
    ProfileNotFoundError,
    ProfileValidationError,
    UsernameTakenError,
    get_profile_by_username,
    update_profile,
)

router = APIRouter(tags=["profiles"])


@router.get("/profiles/{username}", response_model=ProfileResponse)
def read_profile(username: str, db: Session = Depends(get_db)) -> ProfileResponse:
    try:
        profile = get_profile_by_username(db, username)
    except ProfileNotFoundError as exc:
        raise HTTPException(404, str(exc)) from exc
    return to_profile_response(profile)


@router.patch("/me/profile", response_model=ProfileResponse)
def patch_my_profile(
    body: ProfileUpdateRequest,
    user: User = Depends(get_current_user_with_profile),
    db: Session = Depends(get_db),
) -> ProfileResponse:
    try:
        profile = update_profile(
            db,
            user.id,
            body.username,
            body.first_name,
            body.last_name,
            body.display_name,
            body.bio,
        )
    except UsernameTakenError as exc:
        raise HTTPException(409, str(exc)) from exc
    except ProfileValidationError as exc:
        raise HTTPException(400, str(exc)) from exc
    return to_profile_response(profile)
