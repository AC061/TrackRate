# Docker — TrackRate + MusicBrainz

Stack unificado en un solo `docker compose` (misma red Docker).

## Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│  docker compose (trackrate-stack)                           │
│                                                             │
│  MusicBrainz          TrackRate                             │
│  ├── db               ├── trackrate-postgres                │
│  ├── valkey           ├── trackrate-minio                   │
│  ├── search           └── trackrate-api  ──► musicbrainz:5000│
│  └── musicbrainz :5000                                      │
└─────────────────────────────────────────────────────────────┘
```

## Primera instalación (servidor 100.126.35.7)

```bash
git pull   # obtener este compose
chmod +x scripts/stack-setup.sh scripts/stack-reset.sh
./scripts/stack-setup.sh
```

## Si el setup anterior falló — reset limpio

```bash
./scripts/stack-reset.sh
```

## Uso diario

```bash
docker compose up -d
docker compose ps
curl http://100.126.35.7:8000/health
curl -H "User-Agent: TrackRate/1.0" \
  "http://100.126.35.7:5000/ws/2/artist?query=beatles&fmt=json&limit=1"
```

## Variables opcionales (.env en raíz o export)

| Variable | Default |
|----------|---------|
| `TRACKRATE_API_PORT` | 8000 |
| `MINIO_PUBLIC_URL` | http://100.126.35.7:9000 |
| `MUSICBRAINZ_USER_AGENT` | TrackRate/1.0 (dev@trackrate.local) |

Copia `backend/.env.example` → `backend/.env` antes del primer `up`.
