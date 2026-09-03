"""initial schema

Revision ID: 001
Revises:
Create Date: 2026-07-06
"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "001"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

moderation_status = postgresql.ENUM(
    "pending", "approved", "rejected", name="moderation_status", create_type=False
)
music_entity_type = postgresql.ENUM(
    "track", "album", "artist", name="music_entity_type", create_type=False
)
activity_type = postgresql.ENUM(
    "rated", "reviewed", "updated", name="activity_type", create_type=False
)


def upgrade() -> None:
    moderation_status.create(op.get_bind(), checkfirst=True)
    music_entity_type.create(op.get_bind(), checkfirst=True)
    activity_type.create(op.get_bind(), checkfirst=True)

    op.create_table(
        "users",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("email", sa.String(length=320), nullable=False),
        sa.Column("password_hash", sa.String(length=255), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("email"),
    )
    op.create_index("ix_users_email", "users", ["email"])

    op.create_table(
        "profiles",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("username", sa.String(length=30), nullable=False),
        sa.Column("display_name", sa.String(length=100), nullable=True),
        sa.Column("bio", sa.Text(), nullable=True),
        sa.Column("avatar_url", sa.Text(), nullable=True),
        sa.Column("is_admin", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.CheckConstraint("username ~ '^[a-z0-9_]{3,30}$'", name="profiles_username_format"),
        sa.ForeignKeyConstraint(["id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("username"),
    )
    op.create_index("ix_profiles_username", "profiles", ["username"])

    op.create_table(
        "artists",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("name", sa.Text(), nullable=False),
        sa.Column("bio", sa.Text(), nullable=True),
        sa.Column("image_url", sa.Text(), nullable=True),
        sa.Column("submitted_by", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("status", moderation_status, nullable=False, server_default="pending"),
        sa.Column("reviewed_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("reviewed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("rejection_reason", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["submitted_by"], ["profiles.id"]),
        sa.ForeignKeyConstraint(["reviewed_by"], ["profiles.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_artists_name", "artists", ["name"])

    op.create_table(
        "albums",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("title", sa.Text(), nullable=False),
        sa.Column("artist_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("release_year", sa.Integer(), nullable=True),
        sa.Column("cover_url", sa.Text(), nullable=True),
        sa.Column("submitted_by", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("status", moderation_status, nullable=False, server_default="pending"),
        sa.Column("reviewed_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("reviewed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("rejection_reason", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.CheckConstraint(
            "release_year IS NULL OR (release_year >= 1900 AND release_year <= 2100)",
            name="albums_release_year_range",
        ),
        sa.ForeignKeyConstraint(["artist_id"], ["artists.id"]),
        sa.ForeignKeyConstraint(["submitted_by"], ["profiles.id"]),
        sa.ForeignKeyConstraint(["reviewed_by"], ["profiles.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_albums_title", "albums", ["title"])

    op.create_table(
        "tracks",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("title", sa.Text(), nullable=False),
        sa.Column("album_id", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("artist_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("duration_ms", sa.Integer(), nullable=True),
        sa.Column("cover_url", sa.Text(), nullable=True),
        sa.Column("submitted_by", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("status", moderation_status, nullable=False, server_default="pending"),
        sa.Column("reviewed_by", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("reviewed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("rejection_reason", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.CheckConstraint("duration_ms IS NULL OR duration_ms > 0", name="tracks_duration_positive"),
        sa.ForeignKeyConstraint(["album_id"], ["albums.id"]),
        sa.ForeignKeyConstraint(["artist_id"], ["artists.id"]),
        sa.ForeignKeyConstraint(["submitted_by"], ["profiles.id"]),
        sa.ForeignKeyConstraint(["reviewed_by"], ["profiles.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_tracks_title", "tracks", ["title"])

    op.create_table(
        "ratings",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("entity_type", music_entity_type, nullable=False),
        sa.Column("entity_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("rating", sa.Numeric(2, 1), nullable=False),
        sa.Column("review", sa.Text(), nullable=True),
        sa.Column("listened_at", sa.Date(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.CheckConstraint(
            "rating >= 0.5 AND rating <= 5.0 AND mod((rating * 2)::numeric, 1) = 0",
            name="ratings_score_range",
        ),
        sa.ForeignKeyConstraint(["user_id"], ["profiles.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id", "entity_type", "entity_id", name="ratings_user_entity_unique"),
    )

    op.create_table(
        "follows",
        sa.Column("follower_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("following_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.CheckConstraint("follower_id <> following_id", name="follows_no_self"),
        sa.ForeignKeyConstraint(["follower_id"], ["profiles.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["following_id"], ["profiles.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("follower_id", "following_id"),
    )

    op.create_table(
        "activities",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("rating_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("activity_type", activity_type, nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["rating_id"], ["ratings.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["user_id"], ["profiles.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("activities_user_id_created_at_idx", "activities", ["user_id", "created_at"])
    op.create_index("activities_created_at_idx", "activities", ["created_at"])

    op.create_table(
        "lists",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("title", sa.Text(), nullable=False),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("is_public", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("cover_url", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["profiles.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )

    op.create_table(
        "list_items",
        sa.Column("list_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("entity_type", music_entity_type, nullable=False),
        sa.Column("entity_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("position", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["list_id"], ["lists.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("list_id", "entity_type", "entity_id"),
    )
    op.create_index("list_items_list_id_position_idx", "list_items", ["list_id", "position"])


def downgrade() -> None:
    op.drop_index("list_items_list_id_position_idx", table_name="list_items")
    op.drop_table("list_items")
    op.drop_table("lists")
    op.drop_index("activities_created_at_idx", table_name="activities")
    op.drop_index("activities_user_id_created_at_idx", table_name="activities")
    op.drop_table("activities")
    op.drop_table("follows")
    op.drop_table("ratings")
    op.drop_index("ix_tracks_title", table_name="tracks")
    op.drop_table("tracks")
    op.drop_index("ix_albums_title", table_name="albums")
    op.drop_table("albums")
    op.drop_index("ix_artists_name", table_name="artists")
    op.drop_table("artists")
    op.drop_index("ix_profiles_username", table_name="profiles")
    op.drop_table("profiles")
    op.drop_index("ix_users_email", table_name="users")
    op.drop_table("users")
    activity_type.drop(op.get_bind(), checkfirst=True)
    music_entity_type.drop(op.get_bind(), checkfirst=True)
    moderation_status.drop(op.get_bind(), checkfirst=True)
