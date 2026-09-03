# TrackRate API (FastAPI)

Backend desplegable en servidor — catálogo **MusicBrainz** + capa social.

Solo esta carpeta `backend/` debe vivir en el servidor de desarrollo.

## Stack Docker (TrackRate + MusicBrainz)

```
MusicBrainz              TrackRate
├── db                   ├── trackrate-postgres
├── valkey               ├── trackrate-minio
├── search               └── trackrate-api → musicbrainz:5000
└── musicbrainz :5000
```

### Primera instalación

```bash
cd backend
cp .env.example .env
chmod +x scripts/stack-setup.sh scripts/stack-reset.sh
./scripts/stack-setup.sh
```

Sample dump ~15 GB. Tarda bastante.

### Error `Only full data can be loaded in mirror mode`

MusicBrainz estaba en mirror mode. El compose incluye `compose/musicbrainz-standalone.yml` para permitir `-sample`. Tras actualizar:

```bash
docker compose down -v
./scripts/stack-setup.sh
```

### Reset completo (setup fallido)

```bash
./scripts/stack-reset.sh
```

### Uso diario

```bash
docker compose up -d
docker compose ps
docker compose logs -f trackrate-api musicbrainz
```

### Reset solo TrackRate (sin borrar MusicBrainz)

```bash
./scripts/reset-trackrate.sh
```

### Error `env: bash\r: No such file or directory`

Los scripts se editaron en Windows y tienen finales CRLF. En el servidor Ubuntu:

```bash
sed -i 's/\r$//' scripts/*.sh scripts/musicbrainz/*.sh
chmod +x scripts/*.sh scripts/musicbrainz/*.sh
```

Tras `git pull`, `.gitattributes` fuerza LF en `*.sh`.

### Búsqueda vacía en TrackRate

La búsqueda de catálogo usa **Postgres MusicBrainz** (sin Solr). Comprueba:

```bash
docker compose exec db psql -U musicbrainz -d musicbrainz -c "SELECT count(*) FROM artist;"
curl "http://100.126.35.7:8000/catalog/search?q=beatles&type=artist"
```

El WS directo (`:5000/ws/2/artist?query=`) puede devolver `[]` sin indexador Solr — es normal. Usa la API TrackRate.

### Búsqueda WS MusicBrainz vacía (`artists: []`)

Normal sin Solr. TrackRate no depende del indexador. Detalle/lookup por MBID en `:5000/ws/2/artist/{mbid}` sí funciona vía Postgres.

Comprobar datos en MB:

```bash
docker compose exec db psql -U musicbrainz -d musicbrainz -c "SELECT count(*) FROM artist;"
```

Si el count es 0 → el dump no terminó; `./scripts/stack-reset.sh`.

### Error `address already in use` en puerto 5432

Otro Postgres (del host o contenedor previo) usa el 5432. TrackRate ya **no publica** Postgres al host; solo la API se conecta por red interna. Tras actualizar:

```bash
docker compose up -d
```

Si necesitas acceder desde el host (psql, DBeaver), añade en `compose/trackrate.yml` bajo `trackrate-postgres`:

```yaml
ports:
  - "5433:5432"
```

### Diagnóstico

```bash
./scripts/diagnose.sh 100.126.35.7
python scripts/smoke_test.py   # TRACKRATE_API_URL=http://100.126.35.7:8000
```

## URLs (servidor dev Tailscale)

| Servicio | URL |
|----------|-----|
| TrackRate API | http://100.126.35.7:8000 |
| MusicBrainz WS | http://100.126.35.7:5000/ws/2 |
| MinIO | http://100.126.35.7:9000 |
| OpenAPI | http://100.126.35.7:8000/docs |

## Credenciales dev

| Email | Password |
|-------|----------|
| admin@trackrate.dev | TrackRateAdmin123! |

## Scripts

| Script | Descripción |
|--------|-------------|
| `scripts/stack-setup.sh` | Clona MB, sample dump, levanta todo |
| `scripts/stack-reset.sh` | Borra volúmenes y reinstala |
| `scripts/reset-trackrate.sh` | Reset solo Postgres/MinIO/API |
| `scripts/diagnose.sh` | Comprueba API, MB y bases |
| `scripts/smoke_test.py` | Smoke test HTTP |
| `scripts/musicbrainz/setup-prod.sh` | Mirror producción (~100+ GB) |

## Desarrollo local (API sin contenedor)

```bash
docker compose up -d trackrate-postgres trackrate-minio musicbrainz
pip install -r requirements.txt
alembic upgrade head && python -m scripts.seed
uvicorn app.main:app --reload --port 8000
```

## Documentación

- [`docs/musicbrainz-entities.md`](docs/musicbrainz-entities.md) — mapeo entity_type → MBID
