"""MusicBrainz: eliminar catálogo propio y añadir snapshots en capa social.

Revision ID: 005
Revises: 004
"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "005"
down_revision: Union[str, None] = "004"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.drop_table("track_samples")
    op.drop_table("catalog_contributors")
    op.drop_table("tracks")
    op.drop_table("albums")
    op.drop_table("artists")
    op.drop_table("record_labels")

    op.add_column("ratings", sa.Column("entity_title", sa.Text(), nullable=True))
    op.add_column("ratings", sa.Column("entity_subtitle", sa.Text(), nullable=True))
    op.add_column("ratings", sa.Column("entity_image_url", sa.Text(), nullable=True))

    op.add_column("list_items", sa.Column("entity_title", sa.Text(), nullable=True))
    op.add_column("list_items", sa.Column("entity_subtitle", sa.Text(), nullable=True))


def downgrade() -> None:
    op.drop_column("list_items", "entity_subtitle")
    op.drop_column("list_items", "entity_title")
    op.drop_column("ratings", "entity_image_url")
    op.drop_column("ratings", "entity_subtitle")
    op.drop_column("ratings", "entity_title")

    op.create_table(
        "record_labels",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("name", sa.Text(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("name"),
    )
    op.create_index("ix_record_labels_name", "record_labels", ["name"])

    moderation_status = sa.Enum("pending", "approved", "rejected", name="moderation_status")
    music_entity_type = sa.Enum("track", "album", "artist", name="music_entity_type")
    contributor_role = sa.Enum(
        "producer",
        "featured_artist",
        "composer",
        "lyricist",
        "engineer",
        "mixer",
        "mastering",
        "other",
        name="contributor_role",
    )

    op.create_table(
        "artists",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("name", sa.Text(), nullable=False),
        sa.Column("bio", sa.Text(), nullable=True),
        sa.Column("image_url", sa.Text(), nullable=True),
        sa.Column("submitted_by", sa.UUID(), nullable=False),
        sa.Column("status", moderation_status, nullable=False),
        sa.Column("reviewed_by", sa.UUID(), nullable=True),
        sa.Column("reviewed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("rejection_reason", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["submitted_by"], ["profiles.id"]),
        sa.ForeignKeyConstraint(["reviewed_by"], ["profiles.id"]),
        sa.PrimaryKeyConstraint("id"),
    )

    op.create_table(
        "albums",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("title", sa.Text(), nullable=False),
        sa.Column("artist_id", sa.UUID(), nullable=False),
        sa.Column("release_year", sa.Integer(), nullable=True),
        sa.Column("cover_url", sa.Text(), nullable=True),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("label_id", sa.UUID(), nullable=True),
        sa.Column("submitted_by", sa.UUID(), nullable=False),
        sa.Column("status", moderation_status, nullable=False),
        sa.Column("reviewed_by", sa.UUID(), nullable=True),
        sa.Column("reviewed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("rejection_reason", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["artist_id"], ["artists.id"]),
        sa.ForeignKeyConstraint(["label_id"], ["record_labels.id"]),
        sa.ForeignKeyConstraint(["submitted_by"], ["profiles.id"]),
        sa.ForeignKeyConstraint(["reviewed_by"], ["profiles.id"]),
        sa.PrimaryKeyConstraint("id"),
    )

    op.create_table(
        "tracks",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("title", sa.Text(), nullable=False),
        sa.Column("album_id", sa.UUID(), nullable=True),
        sa.Column("artist_id", sa.UUID(), nullable=False),
        sa.Column("duration_ms", sa.Integer(), nullable=True),
        sa.Column("cover_url", sa.Text(), nullable=True),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("label_id", sa.UUID(), nullable=True),
        sa.Column("submitted_by", sa.UUID(), nullable=False),
        sa.Column("status", moderation_status, nullable=False),
        sa.Column("reviewed_by", sa.UUID(), nullable=True),
        sa.Column("reviewed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("rejection_reason", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["album_id"], ["albums.id"]),
        sa.ForeignKeyConstraint(["artist_id"], ["artists.id"]),
        sa.ForeignKeyConstraint(["label_id"], ["record_labels.id"]),
        sa.ForeignKeyConstraint(["submitted_by"], ["profiles.id"]),
        sa.ForeignKeyConstraint(["reviewed_by"], ["profiles.id"]),
        sa.PrimaryKeyConstraint("id"),
    )

    op.create_table(
        "catalog_contributors",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("entity_type", music_entity_type, nullable=False),
        sa.Column("entity_id", sa.UUID(), nullable=False),
        sa.Column("artist_id", sa.UUID(), nullable=False),
        sa.Column("role", contributor_role, nullable=False),
        sa.Column("notes", sa.Text(), nullable=True),
        sa.Column("sort_order", sa.Integer(), nullable=False),
        sa.ForeignKeyConstraint(["artist_id"], ["artists.id"]),
        sa.PrimaryKeyConstraint("id"),
    )

    op.create_table(
        "track_samples",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("track_id", sa.UUID(), nullable=False),
        sa.Column("sampled_track_id", sa.UUID(), nullable=False),
        sa.Column("notes", sa.Text(), nullable=True),
        sa.Column("sort_order", sa.Integer(), nullable=False),
        sa.ForeignKeyConstraint(["sampled_track_id"], ["tracks.id"]),
        sa.ForeignKeyConstraint(["track_id"], ["tracks.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
