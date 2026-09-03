"""Cliente HTTP para MusicBrainz Web Service (mirror local o público)."""

from __future__ import annotations

from uuid import UUID

import httpx

from app.config import settings

_MB_RESOURCE = {
    "artist": "artist",
    "album": "release-group",
    "track": "recording",
}

_SEARCH_FIELD = {
    "artist": "artists",
    "album": "release-groups",
    "track": "recordings",
}

_LOOKUP_INC = {
    "artist": "release-groups+url-rels",
    "album": "artists+releases",
    "track": "artists+releases",
}


class MusicBrainzError(Exception):
    pass


class MusicBrainzNotFoundError(MusicBrainzError):
    pass


def _mbid_str(mbid: UUID) -> str:
    return str(mbid)


class MusicBrainzClient:
    def __init__(self) -> None:
        self._base = settings.musicbrainz_ws_url.rstrip("/")
        self._user_agent = settings.musicbrainz_user_agent
        self._timeout = settings.musicbrainz_timeout_seconds

    def _headers(self) -> dict[str, str]:
        return {
            "User-Agent": self._user_agent,
            "Accept": "application/json",
        }

    def _get(self, path: str, *, params: dict | None = None) -> dict:
        url = f"{self._base}{path}"
        try:
            with httpx.Client(timeout=self._timeout) as client:
                response = client.get(url, params=params or {}, headers=self._headers())
        except httpx.HTTPError as exc:
            raise MusicBrainzError("No se pudo contactar MusicBrainz") from exc

        if response.status_code == 404:
            raise MusicBrainzNotFoundError("Entidad no encontrada en MusicBrainz")
        if response.status_code >= 400:
            raise MusicBrainzError(f"MusicBrainz respondió {response.status_code}")
        return response.json()

    def search(
        self,
        query: str,
        entity_type: str | None = None,
        *,
        limit: int = 25,
    ) -> list[dict]:
        clean = query.strip()
        if not clean:
            return []

        types = [entity_type] if entity_type else ["artist", "album", "track"]
        results: list[dict] = []

        for etype in types:
            resource = _MB_RESOURCE.get(etype)
            field = _SEARCH_FIELD.get(etype)
            if resource is None or field is None:
                continue
            payload = self._get(
                f"/{resource}",
                params={"query": clean, "fmt": "json", "limit": limit},
            )
            for item in payload.get(field, []):
                item["_trackrate_type"] = etype
                results.append(item)
            if entity_type is not None:
                break

        return results[:limit]

    def lookup(self, entity_type: str, mbid: UUID) -> dict:
        resource = _MB_RESOURCE.get(entity_type)
        if resource is None:
            raise MusicBrainzError("Tipo de entidad no válido")
        inc = _LOOKUP_INC.get(entity_type, "")
        params: dict[str, str] = {"fmt": "json"}
        if inc:
            params["inc"] = inc
        return self._get(f"/{resource}/{_mbid_str(mbid)}", params=params)

    def exists(self, entity_type: str, mbid: UUID) -> bool:
        try:
            self.lookup(entity_type, mbid)
            return True
        except MusicBrainzNotFoundError:
            return False
        except MusicBrainzError:
            return False


def parse_mbid(raw: str) -> UUID:
    return UUID(raw)


def artist_credit(entity: dict) -> str | None:
    if "artist-credit" in entity and entity["artist-credit"]:
        parts = entity["artist-credit"]
        if isinstance(parts, list) and parts:
            name = parts[0].get("name") or parts[0].get("artist", {}).get("name")
            return name
    if "artist-credit-name" in entity:
        return entity["artist-credit-name"]
    return None


def first_release_date(entity: dict) -> int | None:
    raw = entity.get("first-release-date") or entity.get("date")
    if not raw:
        return None
    year_str = str(raw)[:4]
    if year_str.isdigit():
        return int(year_str)
    return None


def duration_ms(entity: dict) -> int | None:
    length = entity.get("length")
    if length is None:
        return None
    try:
        return int(length)
    except (TypeError, ValueError):
        return None
