import enum
import uuid
from datetime import date, datetime

from sqlalchemy import (
    Boolean,
    CheckConstraint,
    Date,
    DateTime,
    Enum,
    ForeignKey,
    Index,
    Integer,
    Numeric,
    String,
    Text,
    UniqueConstraint,
    func,
)
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base


class ModerationStatus(str, enum.Enum):
    PENDING = "pending"
    APPROVED = "approved"
    REJECTED = "rejected"


class MusicEntityType(str, enum.Enum):
    TRACK = "track"
    ALBUM = "album"
    ARTIST = "artist"


class ActivityType(str, enum.Enum):
    RATED = "rated"
    REVIEWED = "reviewed"
    UPDATED = "updated"


class ContributorRole(str, enum.Enum):
    PRODUCER = "producer"
    FEATURED_ARTIST = "featured_artist"
    COMPOSER = "composer"
    LYRICIST = "lyricist"
    ENGINEER = "engineer"
    MIXER = "mixer"
    MASTERING = "mastering"
    OTHER = "other"


CONTRIBUTOR_ROLE_LABELS: dict[ContributorRole, str] = {
    ContributorRole.PRODUCER: "Productor",
    ContributorRole.FEATURED_ARTIST: "Artista invitado",
    ContributorRole.COMPOSER: "Compositor",
    ContributorRole.LYRICIST: "Letrista",
    ContributorRole.ENGINEER: "Ingeniero de sonido",
    ContributorRole.MIXER: "Mezcla",
    ContributorRole.MASTERING: "Masterización",
    ContributorRole.OTHER: "Otro",
}


def _pg_enum(enum_cls: type[enum.Enum], name: str) -> Enum:
    return Enum(
        enum_cls,
        name=name,
        values_callable=lambda members: [member.value for member in members],
    )


class User(Base):
    __tablename__ = "users"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    email: Mapped[str] = mapped_column(String(320), unique=True, nullable=False, index=True)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    profile: Mapped["Profile"] = relationship(back_populates="user", uselist=False)


class Profile(Base):
    __tablename__ = "profiles"
    __table_args__ = (
        CheckConstraint("username ~ '^[a-z0-9_]{3,30}$'", name="profiles_username_format"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), primary_key=True
    )
    username: Mapped[str] = mapped_column(String(30), unique=True, nullable=False, index=True)
    display_name: Mapped[str | None] = mapped_column(String(100))
    bio: Mapped[str | None] = mapped_column(Text)
    avatar_url: Mapped[str | None] = mapped_column(Text)
    is_admin: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    user: Mapped["User"] = relationship(back_populates="profile")


class RecordLabel(Base):
    __tablename__ = "record_labels"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    name: Mapped[str] = mapped_column(Text, nullable=False, unique=True, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class Artist(Base):
    __tablename__ = "artists"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    name: Mapped[str] = mapped_column(Text, nullable=False, index=True)
    bio: Mapped[str | None] = mapped_column(Text)
    image_url: Mapped[str | None] = mapped_column(Text)
    submitted_by: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("profiles.id"))
    status: Mapped[ModerationStatus] = mapped_column(
        _pg_enum(ModerationStatus, "moderation_status"), default=ModerationStatus.PENDING
    )
    reviewed_by: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("profiles.id"))
    reviewed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    rejection_reason: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )


class Album(Base):
    __tablename__ = "albums"
    __table_args__ = (
        CheckConstraint(
            "release_year IS NULL OR (release_year >= 1900 AND release_year <= 2100)",
            name="albums_release_year_range",
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    title: Mapped[str] = mapped_column(Text, nullable=False, index=True)
    artist_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("artists.id"))
    release_year: Mapped[int | None] = mapped_column(Integer)
    cover_url: Mapped[str | None] = mapped_column(Text)
    description: Mapped[str | None] = mapped_column(Text)
    label_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("record_labels.id"))
    submitted_by: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("profiles.id"))
    status: Mapped[ModerationStatus] = mapped_column(
        _pg_enum(ModerationStatus, "moderation_status"), default=ModerationStatus.PENDING
    )
    reviewed_by: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("profiles.id"))
    reviewed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    rejection_reason: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )


class Track(Base):
    __tablename__ = "tracks"
    __table_args__ = (
        CheckConstraint("duration_ms IS NULL OR duration_ms > 0", name="tracks_duration_positive"),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    title: Mapped[str] = mapped_column(Text, nullable=False, index=True)
    album_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("albums.id"))
    artist_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("artists.id"))
    duration_ms: Mapped[int | None] = mapped_column(Integer)
    cover_url: Mapped[str | None] = mapped_column(Text)
    description: Mapped[str | None] = mapped_column(Text)
    label_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("record_labels.id"))
    submitted_by: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("profiles.id"))
    status: Mapped[ModerationStatus] = mapped_column(
        _pg_enum(ModerationStatus, "moderation_status"), default=ModerationStatus.PENDING
    )
    reviewed_by: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), ForeignKey("profiles.id"))
    reviewed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    rejection_reason: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )


class CatalogContributor(Base):
    __tablename__ = "catalog_contributors"
    __table_args__ = (
        UniqueConstraint(
            "entity_type", "entity_id", "artist_id", "role", name="catalog_contributors_unique"
        ),
        Index("ix_catalog_contributors_entity", "entity_type", "entity_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    entity_type: Mapped[MusicEntityType] = mapped_column(_pg_enum(MusicEntityType, "music_entity_type"))
    entity_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False)
    artist_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("artists.id"))
    role: Mapped[ContributorRole] = mapped_column(_pg_enum(ContributorRole, "contributor_role"))
    notes: Mapped[str | None] = mapped_column(Text)
    sort_order: Mapped[int] = mapped_column(Integer, default=0, nullable=False)


class TrackSample(Base):
    __tablename__ = "track_samples"
    __table_args__ = (
        UniqueConstraint("track_id", "sampled_track_id", name="track_samples_unique"),
        CheckConstraint("track_id <> sampled_track_id", name="track_samples_no_self"),
        Index("ix_track_samples_track_id", "track_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    track_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("tracks.id"))
    sampled_track_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("tracks.id"))
    notes: Mapped[str | None] = mapped_column(Text)
    sort_order: Mapped[int] = mapped_column(Integer, default=0, nullable=False)


class Rating(Base):
    __tablename__ = "ratings"
    __table_args__ = (
        UniqueConstraint("user_id", "entity_type", "entity_id", name="ratings_user_entity_unique"),
        CheckConstraint(
            "rating >= 0.5 AND rating <= 5.0 AND mod((rating * 2)::numeric, 1) = 0",
            name="ratings_score_range",
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("profiles.id", ondelete="CASCADE"))
    entity_type: Mapped[MusicEntityType] = mapped_column(_pg_enum(MusicEntityType, "music_entity_type"))
    entity_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True))
    rating: Mapped[float] = mapped_column(Numeric(2, 1), nullable=False)
    review: Mapped[str | None] = mapped_column(Text)
    listened_at: Mapped[date | None] = mapped_column(Date)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )


class Follow(Base):
    __tablename__ = "follows"
    __table_args__ = (
        CheckConstraint("follower_id <> following_id", name="follows_no_self"),
    )

    follower_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("profiles.id", ondelete="CASCADE"), primary_key=True
    )
    following_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("profiles.id", ondelete="CASCADE"), primary_key=True
    )
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class Activity(Base):
    __tablename__ = "activities"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("profiles.id", ondelete="CASCADE"))
    rating_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("ratings.id", ondelete="CASCADE"))
    activity_type: Mapped[ActivityType] = mapped_column(_pg_enum(ActivityType, "activity_type"))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    __table_args__ = (
        Index("activities_user_id_created_at_idx", "user_id", "created_at"),
        Index("activities_created_at_idx", "created_at"),
    )


class MusicList(Base):
    __tablename__ = "lists"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("profiles.id", ondelete="CASCADE"))
    title: Mapped[str] = mapped_column(Text, nullable=False)
    description: Mapped[str | None] = mapped_column(Text)
    is_public: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    cover_url: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )


class ListItem(Base):
    __tablename__ = "list_items"

    list_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("lists.id", ondelete="CASCADE"), primary_key=True
    )
    entity_type: Mapped[MusicEntityType] = mapped_column(
        _pg_enum(MusicEntityType, "music_entity_type"), primary_key=True
    )
    entity_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True)
    position: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    __table_args__ = (Index("list_items_list_id_position_idx", "list_id", "position"),)
