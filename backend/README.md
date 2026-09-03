# TrackRate API (FastAPI)

Catálogo vía **MusicBrainz** + capa social. El Docker Compose unificado está en la **raíz del repo**.

Ver [`../README-DOCKER.md`](../README-DOCKER.md) para levantar MusicBrainz + TrackRate juntos.

## Desde la raíz (recomendado)

```bash
cd ..
./scripts/stack-setup.sh      # primera vez (~15 GB)
docker compose up -d
docker compose logs -f trackrate-api
```

## Reset TrackRate sin borrar MusicBrainz

```bash
./scripts/reset-trackrate.sh
```

## Reset completo (MB + TrackRate)

```bash
../scripts/stack-reset.sh
```

## Desarrollo local de la API (sin Docker para Python)

```bash
docker compose up trackrate-postgres trackrate-minio musicbrainz -d   # desde raíz
cd backend && pip install -r requirements.txt && cp .env.example .env
alembic upgrade head && python -m scripts.seed
uvicorn app.main:app --reload --port 8000
```

## Credenciales dev

| Email | Password |
|-------|----------|
| admin@trackrate.dev | TrackRateAdmin123! |

## Endpoints

Ver [`docs/musicbrainz-entities.md`](docs/musicbrainz-entities.md) y `/docs` en :8000.
