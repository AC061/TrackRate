# MusicBrainz — mapeo de entidades TrackRate

## entity_type → MusicBrainz

| TrackRate API | MusicBrainz | WS path |
|---------------|-------------|---------|
| `artist` | Artist | `/artist/{mbid}` |
| `album` | Release-group | `/release-group/{mbid}` |
| `track` | Recording | `/recording/{mbid}` |

## entity_id

UUID MBID (RFC 4122), mismo formato que `UUID` en Postgres y en la API TrackRate.

Ejemplo artista The Beatles: `8d376a32-873e-4081-84f9-7030a3d5b453`

## Portadas

Cover Art Archive:

- Release-group: `https://coverartarchive.org/release-group/{mbid}/front`
- Artist: `https://coverartarchive.org/artist/{mbid}/front`

## Variables de entorno (backend)

```
MUSICBRAINZ_WS_URL=http://localhost:5000/ws/2
MUSICBRAINZ_USER_AGENT=TrackRate/1.0 (contacto@example.com)
COVER_ART_ARCHIVE_URL=https://coverartarchive.org
```

En Docker Compose con MB en el mismo host: `http://musicbrainz:5000/ws/2` (red compartida).

## Dev (~15 GB)

```bash
./scripts/musicbrainz/setup-dev.sh
```

## Prod

Ver `scripts/musicbrainz/setup-prod.sh` y [musicbrainz-docker README](https://github.com/metabrainz/musicbrainz-docker).
