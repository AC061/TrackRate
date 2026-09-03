from datetime import date, datetime
from uuid import UUID

from pydantic import BaseModel, Field, field_validator


def _validate_rating_score(value: float) -> float:
    if value < 0.5 or value > 5.0:
        raise ValueError("La valoración debe estar entre 0.5 y 5.0")
    if round(value * 2) != value * 2:
        raise ValueError("La valoración debe ser en incrementos de 0.5")
    return value


class RatingResponse(BaseModel):
    id: UUID
    rating: float
    review: str | None = None
    listened_at: date | None = None


class RatingStatsResponse(BaseModel):
    average: float
    count: int


class RatingUpsertRequest(BaseModel):
    entity_type: str = Field(pattern=r"^(artist|album|track)$")
    entity_id: UUID
    rating: float
    review: str | None = Field(default=None, max_length=10000)
    listened_at: date | None = None

    @field_validator("rating")
    @classmethod
    def validate_rating(cls, value: float) -> float:
        return _validate_rating_score(value)


class RatingDetailResponse(BaseModel):
    id: UUID
    entity_type: str
    entity_id: UUID
    rating: float
    review: str | None = None
    listened_at: date | None = None
    entity_title: str | None = None
    entity_subtitle: str | None = None


class ActivityFeedResponse(BaseModel):
    id: UUID
    user_id: UUID
    username: str
    display_name: str | None = None
    avatar_url: str | None = None
    activity_type: str
    created_at: datetime
    rating: float
    review: str | None = None
    entity_type: str
    entity_id: UUID
    entity_title: str | None = None
    entity_subtitle: str | None = None


class FollowingIdResponse(BaseModel):
    following_id: UUID


class FollowCheckResponse(BaseModel):
    follower_id: UUID


class MusicListResponse(BaseModel):
    id: UUID
    title: str
    description: str | None = None
    is_public: bool

    model_config = {"from_attributes": True}


class CreateListRequest(BaseModel):
    title: str = Field(min_length=1, max_length=500)
    description: str | None = Field(default=None, max_length=5000)
    is_public: bool = False


class ListItemResponse(BaseModel):
    list_id: UUID
    entity_type: str
    entity_id: UUID
    position: int
    entity_title: str | None = None
    entity_subtitle: str | None = None


class AddListItemRequest(BaseModel):
    entity_type: str = Field(pattern=r"^(artist|album|track)$")
    entity_id: UUID


class ProfileStatsResponse(BaseModel):
    follower_count: int
    following_count: int
    rating_count: int


class UserRatingStatsResponse(BaseModel):
    total_ratings: int
    average_rating: float
    review_count: int
