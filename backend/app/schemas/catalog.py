from enum import Enum
from uuid import UUID

from pydantic import BaseModel, Field


class ContributorRoleEnum(str, Enum):
    PRODUCER = "producer"
    FEATURED_ARTIST = "featured_artist"
    COMPOSER = "composer"
    LYRICIST = "lyricist"
    ENGINEER = "engineer"
    MIXER = "mixer"
    MASTERING = "mastering"
    OTHER = "other"


class CatalogItemResponse(BaseModel):
    id: UUID
    type: str
    title: str
    subtitle: str | None = None
    image_url: str | None = None
    year: int | None = None


class ContributorResponse(BaseModel):
    artist_id: UUID
    artist_name: str
    role: str
    role_label: str
    notes: str | None = None


class SampleResponse(BaseModel):
    track_id: UUID
    title: str
    artist_name: str
    album_title: str | None = None
    notes: str | None = None


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
    label: str | None = None
    label_id: UUID | None = None
    status: str | None = None
    contributors: list[ContributorResponse] = Field(default_factory=list)
    samples: list[SampleResponse] = Field(default_factory=list)


class TopRatedTrackResponse(BaseModel):
    id: UUID
    title: str
    subtitle: str | None = None
    image_url: str | None = None
    average_rating: float
    rating_count: int


class ContributorInput(BaseModel):
    artist_id: UUID
    role: ContributorRoleEnum
    notes: str | None = Field(default=None, max_length=500)


class SampleInput(BaseModel):
    sampled_track_id: UUID
    notes: str | None = Field(default=None, max_length=500)


class SubmitArtistRequest(BaseModel):
    name: str = Field(min_length=1, max_length=500)
    bio: str | None = Field(default=None, max_length=5000)


class SubmitAlbumRequest(BaseModel):
    title: str = Field(min_length=1, max_length=500)
    artist_id: UUID
    release_year: int | None = Field(default=None, ge=1900, le=2100)
    description: str | None = Field(default=None, max_length=5000)
    label_id: UUID | None = None
    contributors: list[ContributorInput] = Field(default_factory=list, max_length=20)


class SubmitTrackRequest(BaseModel):
    title: str = Field(min_length=1, max_length=500)
    artist_id: UUID
    album_id: UUID | None = None
    duration_ms: int | None = Field(default=None, gt=0)
    description: str | None = Field(default=None, max_length=5000)
    label_id: UUID | None = None
    contributors: list[ContributorInput] = Field(default_factory=list, max_length=20)
    samples: list[SampleInput] = Field(default_factory=list, max_length=10)


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
