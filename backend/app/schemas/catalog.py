from enum import Enum
from uuid import UUID

from pydantic import BaseModel, Field


class CatalogItemResponse(BaseModel):
    id: UUID
    type: str
    title: str
    subtitle: str | None = None
    image_url: str | None = None
    year: int | None = None


class CatalogDetailResponse(BaseModel):
    id: UUID
    type: str
    title: str
    subtitle: str | None = None
    extra: str | None = None
    description: str | None = None
    image_url: str | None = None
    year: int | None = None
    duration_ms: int | None = None
    artist_id: UUID | None = None
    album_id: UUID | None = None


class TopRatedEntityResponse(BaseModel):
    id: UUID
    type: str
    title: str
    subtitle: str | None = None
    image_url: str | None = None
    average_rating: float
    rating_count: int


class CoverResponse(BaseModel):
    url: str | None = None
