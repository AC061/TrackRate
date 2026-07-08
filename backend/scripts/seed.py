"""Seed de desarrollo: admin + catálogo de prueba."""

from uuid import UUID

from sqlalchemy import select

from app.core.security import hash_password
from app.db.session import SessionLocal
from app.models import Album, Artist, ModerationStatus, Profile, Track, User

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
            display_name="TrackRate Admin",
            bio="Cuenta administrador de desarrollo.",
            is_admin=True,
        )
        db.add(user)
        db.add(profile)
        db.flush()

        artists = [
            Artist(
                id=UUID("a0000001-0000-4000-8000-000000000001"),
                name="Radiohead",
                bio="Banda británica de rock alternativo.",
                submitted_by=ADMIN_ID,
                status=ModerationStatus.APPROVED,
                reviewed_by=ADMIN_ID,
            ),
            Artist(
                id=UUID("a0000001-0000-4000-8000-000000000002"),
                name="Bjork",
                bio="Cantautora islandesa.",
                submitted_by=ADMIN_ID,
                status=ModerationStatus.APPROVED,
                reviewed_by=ADMIN_ID,
            ),
        ]
        db.add_all(artists)
        db.flush()

        albums = [
            Album(
                id=UUID("a0000002-0000-4000-8000-000000000001"),
                title="OK Computer",
                artist_id=artists[0].id,
                release_year=1997,
                submitted_by=ADMIN_ID,
                status=ModerationStatus.APPROVED,
                reviewed_by=ADMIN_ID,
            ),
            Album(
                id=UUID("a0000002-0000-4000-8000-000000000002"),
                title="Vespertine",
                artist_id=artists[1].id,
                release_year=2001,
                submitted_by=ADMIN_ID,
                status=ModerationStatus.APPROVED,
                reviewed_by=ADMIN_ID,
            ),
        ]
        db.add_all(albums)
        db.flush()

        tracks = [
            Track(
                id=UUID("a0000003-0000-4000-8000-000000000001"),
                title="Paranoid Android",
                album_id=albums[0].id,
                artist_id=artists[0].id,
                duration_ms=383000,
                submitted_by=ADMIN_ID,
                status=ModerationStatus.APPROVED,
                reviewed_by=ADMIN_ID,
            ),
            Track(
                id=UUID("a0000003-0000-4000-8000-000000000002"),
                title="Hidden Place",
                album_id=albums[1].id,
                artist_id=artists[1].id,
                duration_ms=337000,
                submitted_by=ADMIN_ID,
                status=ModerationStatus.APPROVED,
                reviewed_by=ADMIN_ID,
            ),
        ]
        db.add_all(tracks)
        db.commit()
        print(f"Seed OK: {ADMIN_EMAIL} / {ADMIN_PASSWORD}")
    finally:
        db.close()


if __name__ == "__main__":
    seed()
