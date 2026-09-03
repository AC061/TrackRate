"""Cover Art Archive — URLs de portadas para entidades MusicBrainz."""

from __future__ import annotations

from uuid import UUID

import httpx

from app.config import settings

_CAA_PATH = {
    "artist": "artist",
    "album": "release-group",
    "track": "release",
}


def cover_art_url(entity_type: str, mbid: UUID, *, release_mbid: UUID | None = None) -> str | None:
    """Devuelve URL de imagen front si existe en Cover Art Archive."""
    if entity_type == "track" and release_mbid is not None:
        path_type = "release"
        path_id = release_mbid
    else:
        path_type = _CAA_PATH.get(entity_type)
        if path_type is None:
            return None
        path_id = mbid

    base = settings.cover_art_archive_url.rstrip("/")
    url = f"{base}/{path_type}/{path_id}"
    try:
        with httpx.Client(timeout=10.0, follow_redirects=True) as client:
            response = client.get(f"{url}/front")
            if response.status_code == 404:
                return None
            if response.status_code >= 400:
                return None
            return str(response.url)
    except httpx.HTTPError:
        return None
