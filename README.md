# TrackRate

App Android tipo Letterboxd para música, con backend propio FastAPI.

**Curso:** 1GS132 — Ingeniería de Desarrollo de Software Móvil  
**Integrantes:** ver comentario ENT-1 en [`MainActivity.kt`](app/src/main/java/com/example/trackrate/MainActivity.kt)

## Arquitectura

```
Android (Kotlin, MVVM, Hilt, Navigation Component)
    ├── 5 Activities: Login, Main, Detail, Submit, Change Password
    ├── Fragments: Home, Search, Diary, Profile, Moderación, Listas, …
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

### 3. Imágenes en emulador (recomendado)

Para que avatares y portadas carguen desde MinIO, en `backend/.env`:

```properties
MINIO_PUBLIC_URL=http://10.0.2.2:9000
```

Reinicia el contenedor API tras cambiar `.env`:

```powershell
docker compose up -d --build api
```

## Subida de imágenes (Android)

La app permite subir imágenes en estas pantallas:

| Pantalla | Qué sube |
|----------|----------|
| **Ajustes** | Avatar de perfil |
| **Añadir al catálogo** | Imagen opcional al enviar artista/álbum/canción |
| **Mis envíos** | Imagen a un envío ya creado |
| **Detalle de lista** | Portada de la lista |

Formatos: JPEG, PNG, WebP (máx. 10 MB).

## Estructura del repo

```
TrackRate/
├── app/                  # Android (Kotlin) — 4 Activities + Fragments
├── backend/              # FastAPI + Alembic + Docker
├── docs/
│   ├── LICENSES.md       # Licencias y atribución (checklist II)
│   ├── ASSETS.md         # Logos e iconografía
│   ├── icons.md          # Convención MDI
│   └── legacy-supabase/  # Esquema Supabase archivado (referencia)
├── logo_icon.svg         # Fuente diseño — icono
├── logo_full.svg         # Fuente diseño — logotipo
└── local.properties.example
```

## Licencias y recursos de terceros

Ver [`docs/LICENSES.md`](docs/LICENSES.md) para el detalle de iconos MDI, dependencias Android/backend e infraestructura Docker.

## Documentación

- Backend API: [`backend/README.md`](backend/README.md)
- Supabase legacy: [`docs/legacy-supabase/README.md`](docs/legacy-supabase/README.md)

## Dispositivo físico

Usa la IP LAN de tu PC en lugar de `10.0.2.2`:

```properties
API_BASE_URL=http://192.168.x.x:8000
```

Asegúrate de que el firewall permita los puertos 8000 (API) y 9000 (MinIO).
