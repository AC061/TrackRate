# TrackRate

App Android tipo Letterboxd para música, con backend propio FastAPI.

## Arquitectura

```
Android (Kotlin, MVVM, Hilt)
    └── Ktor Client + JWT
            └── FastAPI (Python)
                    ├── PostgreSQL
                    └── MinIO (imágenes)
```

## Inicio rápido

### 1. Backend (Docker)

```powershell
cd backend
copy .env.example .env
docker compose up --build
```

- API: http://localhost:8000/docs
- MinIO console: http://localhost:9001 (`minioadmin` / `minioadmin`)

**Admin dev (seed automático):**

| Campo | Valor |
|-------|--------|
| Email | `admin@trackrate.dev` |
| Password | `TrackRateAdmin123!` |
| Username | `admin` |

### 2. Android

Copia `local.properties.example` → `local.properties` y configura:

```properties
sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk
API_BASE_URL=http://10.0.2.2:8000
```

`10.0.2.2` apunta al localhost del PC desde el emulador Android.

Abre el proyecto en Android Studio, Sync Gradle y Run.

### 3. Imágenes en emulador (opcional)

Para que avatares y portadas carguen desde MinIO, en `backend/.env`:

```properties
MINIO_PUBLIC_URL=http://10.0.2.2:9000
```

Reinicia el contenedor API tras cambiar `.env`.

## Estructura del repo

```
TrackRate/
├── app/                  # Android (Kotlin)
├── backend/              # FastAPI + Alembic + Docker
├── docs/
│   └── legacy-supabase/  # Esquema Supabase archivado (referencia)
└── local.properties.example
```

## Documentación

- Backend API: [`backend/README.md`](backend/README.md)
- Supabase legacy: [`docs/legacy-supabase/README.md`](docs/legacy-supabase/README.md)

## Dispositivo físico

Usa la IP LAN de tu PC en lugar de `10.0.2.2`:

```properties
API_BASE_URL=http://192.168.x.x:8000
```

Asegúrate de que el firewall permita los puertos 8000 (API) y 9000 (MinIO).
