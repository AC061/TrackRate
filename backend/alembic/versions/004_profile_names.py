"""profile first_name and last_name

Revision ID: 004
Revises: 003
Create Date: 2026-07-15
"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "004"
down_revision: Union[str, None] = "003"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "profiles",
        sa.Column("first_name", sa.String(length=100), server_default="", nullable=False),
    )
    op.add_column(
        "profiles",
        sa.Column("last_name", sa.String(length=100), server_default="", nullable=False),
    )
    op.alter_column("profiles", "first_name", server_default=None)
    op.alter_column("profiles", "last_name", server_default=None)


def downgrade() -> None:
    op.drop_column("profiles", "last_name")
    op.drop_column("profiles", "first_name")
