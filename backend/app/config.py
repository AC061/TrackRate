from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    database_url: str = "postgresql+psycopg://trackrate:trackrate@localhost:5432/trackrate"
    jwt_secret: str = "dev-secret-change-me"
    jwt_algorithm: str = "HS256"
    jwt_expire_minutes: int = 60 * 24 * 7  # 7 days

    minio_endpoint: str = "localhost:9000"
    minio_access_key: str = "minioadmin"
    minio_secret_key: str = "minioadmin"
    minio_secure: bool = False
    minio_public_url: str = "http://localhost:9000"

    cors_origins: str = "*"

    musicbrainz_ws_url: str = "http://localhost:5000/ws/2"
    musicbrainz_database_url: str = (
        "postgresql+psycopg://musicbrainz:musicbrainz@db:5432/musicbrainz_db"
    )
    musicbrainz_user_agent: str = "TrackRate/1.0 (dev@trackrate.local)"
    musicbrainz_timeout_seconds: float = 15.0
    cover_art_archive_url: str = "https://coverartarchive.org"


settings = Settings()
