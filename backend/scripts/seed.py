"""Seed de desarrollo: solo cuenta administrador."""

from uuid import UUID

from sqlalchemy import select

from app.core.security import hash_password
from app.db.session import SessionLocal
from app.models import Profile, User

ADMIN_ID = UUID("a0000000-0000-4000-8000-000000000001")
ADMIN_EMAIL = "admin@trackrate.dev"
ADMIN_PASSWORD = "TrackRateAdmin123!"


def seed() -> None:
    db = SessionLocal()
    try:
        existing = db.scalar(select(User).where(User.email == ADMIN_EMAIL))
        if existing:
            print("Seed omitido: admin@trackrate.dev ya existe")
            return

        user = User(
            id=ADMIN_ID,
            email=ADMIN_EMAIL,
            password_hash=hash_password(ADMIN_PASSWORD),
        )
        profile = Profile(
            id=ADMIN_ID,
            username="admin",
            first_name="TrackRate",
            last_name="Admin",
            display_name="TrackRate Admin",
            bio="Cuenta administrador de desarrollo.",
            is_admin=True,
        )
        db.add(user)
        db.add(profile)
        db.commit()
        print(f"Seed OK: {ADMIN_EMAIL} / {ADMIN_PASSWORD}")
        print("Catálogo: MusicBrainz (configura MUSICBRAINZ_WS_URL en .env)")
    finally:
        db.close()


if __name__ == "__main__":
    seed()
