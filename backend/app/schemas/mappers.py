from app.models import User
from app.schemas.auth import ProfileResponse, TokenResponse, UserResponse


def to_profile_response(profile) -> ProfileResponse:
    return ProfileResponse.model_validate(profile)


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
