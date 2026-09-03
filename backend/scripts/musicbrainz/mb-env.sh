# MusicBrainz Docker usa la base "musicbrainz_db" (ver build/musicbrainz/DBDefs.pm).
# Las tablas viven en el schema "musicbrainz".
export MB_DB="${MUSICBRAINZ_POSTGRES_DATABASE:-musicbrainz_db}"
export MB_SCHEMA="${MUSICBRAINZ_DB_SCHEMA:-musicbrainz}"
