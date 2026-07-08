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


class SubmitArtistRequest(BaseModel):
    name: str = Field(min_length=1, max_length=500)
    bio: str | None = Field(default=None, max_length=5000)


class SubmitAlbumRequest(BaseModel):
    title: str = Field(min_length=1, max_length=500)
    artist_id: UUID
    release_year: int | None = Field(default=None, ge=1900, le=2100)


class SubmitTrackRequest(BaseModel):
    title: str = Field(min_length=1, max_length=500)
    artist_id: UUID
    album_id: UUID | None = None
    duration_ms: int | None = Field(default=None, gt=0)


class CatalogSubmissionResponse(BaseModel):
    id: UUID
    type: str
    title: str
    subtitle: str | None = None
    status: str
    rejection_reason: str | None = None


class ModerationActionRequest(BaseModel):
    action: str = Field(pattern=r"^(approve|reject)$")
    rejection_reason: str | None = Field(default=None, max_length=2000)


class SetAdminRequest(BaseModel):
    make_admin: bool
