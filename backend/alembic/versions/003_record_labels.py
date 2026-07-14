"""record labels table + label_id FK

Revision ID: 003
Revises: 002
Create Date: 2026-07-14
"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "003"
down_revision: Union[str, None] = "002"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "record_labels",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("name", sa.Text(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("name"),
    )
    op.create_index("ix_record_labels_name", "record_labels", ["name"])

    op.add_column(
        "albums",
        sa.Column("label_id", postgresql.UUID(as_uuid=True), nullable=True),
    )
    op.add_column(
        "tracks",
        sa.Column("label_id", postgresql.UUID(as_uuid=True), nullable=True),
    )
    op.create_foreign_key(
        "albums_label_id_fkey", "albums", "record_labels", ["label_id"], ["id"]
    )
    op.create_foreign_key(
        "tracks_label_id_fkey", "tracks", "record_labels", ["label_id"], ["id"]
    )

    conn = op.get_bind()
    labels = conn.execute(
        sa.text(
            """
            SELECT DISTINCT TRIM(label) AS name
            FROM (
                SELECT label FROM albums WHERE label IS NOT NULL AND TRIM(label) <> ''
                UNION
                SELECT label FROM tracks WHERE label IS NOT NULL AND TRIM(label) <> ''
            ) AS all_labels
            """
        )
    ).fetchall()
    for (name,) in labels:
        label_id = conn.execute(
            sa.text("SELECT gen_random_uuid()")
        ).scalar_one()
        conn.execute(
            sa.text("INSERT INTO record_labels (id, name) VALUES (:id, :name)"),
            {"id": label_id, "name": name},
        )
        conn.execute(
            sa.text(
                "UPDATE albums SET label_id = :label_id WHERE TRIM(label) = :name"
            ),
            {"label_id": label_id, "name": name},
        )
        conn.execute(
            sa.text(
                "UPDATE tracks SET label_id = :label_id WHERE TRIM(label) = :name"
            ),
            {"label_id": label_id, "name": name},
        )

    op.drop_column("albums", "label")
    op.drop_column("tracks", "label")


def downgrade() -> None:
    op.add_column("albums", sa.Column("label", sa.Text(), nullable=True))
    op.add_column("tracks", sa.Column("label", sa.Text(), nullable=True))

    conn = op.get_bind()
    conn.execute(
        sa.text(
            """
            UPDATE albums a SET label = rl.name
            FROM record_labels rl WHERE a.label_id = rl.id
            """
        )
    )
    conn.execute(
        sa.text(
            """
            UPDATE tracks t SET label = rl.name
            FROM record_labels rl WHERE t.label_id = rl.id
            """
        )
    )

    op.drop_constraint("tracks_label_id_fkey", "tracks", type_="foreignkey")
    op.drop_constraint("albums_label_id_fkey", "albums", type_="foreignkey")
    op.drop_column("tracks", "label_id")
    op.drop_column("albums", "label_id")
    op.drop_index("ix_record_labels_name", table_name="record_labels")
    op.drop_table("record_labels")
