"""catalog metadata: description, label, contributors, samples

Revision ID: 002
Revises: 001
Create Date: 2026-07-14
"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "002"
down_revision: Union[str, None] = "001"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

contributor_role = postgresql.ENUM(
    "producer",
    "featured_artist",
    "composer",
    "lyricist",
    "engineer",
    "mixer",
    "mastering",
    "other",
    name="contributor_role",
    create_type=False,
)
music_entity_type = postgresql.ENUM(
    "track", "album", "artist", name="music_entity_type", create_type=False
)


def upgrade() -> None:
    contributor_role.create(op.get_bind(), checkfirst=True)

    op.add_column("albums", sa.Column("description", sa.Text(), nullable=True))
    op.add_column("albums", sa.Column("label", sa.Text(), nullable=True))
    op.add_column("tracks", sa.Column("description", sa.Text(), nullable=True))
    op.add_column("tracks", sa.Column("label", sa.Text(), nullable=True))

    op.create_table(
        "catalog_contributors",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("entity_type", music_entity_type, nullable=False),
        sa.Column("entity_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("artist_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("role", contributor_role, nullable=False),
        sa.Column("notes", sa.Text(), nullable=True),
        sa.Column("sort_order", sa.Integer(), nullable=False, server_default="0"),
        sa.ForeignKeyConstraint(["artist_id"], ["artists.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint(
            "entity_type", "entity_id", "artist_id", "role", name="catalog_contributors_unique"
        ),
    )
    op.create_index(
        "ix_catalog_contributors_entity",
        "catalog_contributors",
        ["entity_type", "entity_id"],
    )

    op.create_table(
        "track_samples",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("track_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("sampled_track_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("notes", sa.Text(), nullable=True),
        sa.Column("sort_order", sa.Integer(), nullable=False, server_default="0"),
        sa.CheckConstraint("track_id <> sampled_track_id", name="track_samples_no_self"),
        sa.ForeignKeyConstraint(["sampled_track_id"], ["tracks.id"]),
        sa.ForeignKeyConstraint(["track_id"], ["tracks.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("track_id", "sampled_track_id", name="track_samples_unique"),
    )
    op.create_index("ix_track_samples_track_id", "track_samples", ["track_id"])


def downgrade() -> None:
    op.drop_index("ix_track_samples_track_id", table_name="track_samples")
    op.drop_table("track_samples")
    op.drop_index("ix_catalog_contributors_entity", table_name="catalog_contributors")
    op.drop_table("catalog_contributors")
    op.drop_column("tracks", "label")
    op.drop_column("tracks", "description")
    op.drop_column("albums", "label")
    op.drop_column("albums", "description")
    contributor_role.drop(op.get_bind(), checkfirst=True)
