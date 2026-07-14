from app.models import User
from app.schemas.auth import ProfileResponse, TokenResponse, UserResponse


from app.services.storage_service import normalize_stored_media_url


def to_profile_response(profile) -> ProfileResponse:
    response = ProfileResponse.model_validate(profile)
    return response.model_copy(
        update={"avatar_url": normalize_stored_media_url(response.avatar_url)}
    )


def to_user_response(user: User) -> UserResponse:
    if user.profile is None:
        raise ValueError("User profile not loaded")
    return UserResponse(
        id=user.id,
        email=user.email,
        profile=to_profile_response(user.profile),
    )


def to_token_response(user: User, access_token: str) -> TokenResponse:
    return TokenResponse(access_token=access_token, user=to_user_response(user))
