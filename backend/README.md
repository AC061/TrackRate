# TrackRate API (FastAPI)

Backend REST que reemplaza Supabase Auth + PostgREST.

## Requisitos

- Python 3.12+
- Docker (opcional, recomendado para Postgres + MinIO)

## Inicio rápido con Docker

```bash
cd backend
copy .env.example .env
docker compose up --build
```

La API quedará en **http://localhost:8000**  
Documentación interactiva: **http://localhost:8000/docs**

El contenedor ejecuta automáticamente migraciones y seed de desarrollo.

### Credenciales dev (seed)

| Campo | Valor |
|-------|--------|
| Email | `admin@trackrate.dev` |
| Password | `TrackRateAdmin123!` |
| Username | `admin` |

## Desarrollo local (sin Docker para la API)

```bash
cd backend
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env

# Levanta solo Postgres y MinIO
docker compose up postgres minio -d

alembic upgrade head
python -m scripts.seed
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

## Endpoints implementados (v1.0)

### Auth y perfiles
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/health` | Health check |
| POST | `/auth/register` | Registro → JWT inmediato |
| POST | `/auth/login` | Login → JWT |
| GET | `/auth/me` | Usuario autenticado |
| GET | `/profiles/{username}` | Perfil público |
| PATCH | `/me/profile` | Actualizar perfil propio |
| GET | `/users/{id}/stats` | Seguidores, seguidos, valoraciones |
| GET | `/users/{id}/rating-stats` | Promedio y reseñas del usuario |

### Catálogo y moderación
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/catalog/search` | Búsqueda en catálogo aprobado |
| GET | `/catalog/{type}/{id}` | Detalle aprobado |
| GET | `/catalog/{type}/{id}/rating-stats` | Promedio y conteo de valoraciones |
| GET | `/catalog/artists/{id}/albums` | Álbumes de un artista |
| POST | `/catalog/artists\|albums\|tracks` | Enviar al catálogo (`pending`) |
| GET | `/me/submissions` | Mis envíos |
| GET | `/admin/moderation/pending` | Cola de moderación (admin) |
| PATCH | `/admin/moderation/{type}/{id}` | Aprobar/rechazar (admin) |
| PATCH | `/admin/users/{username}` | Promover/degradar admin |

### Social (Fase 2c)
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/me/ratings` | Mi valoración de una entidad |
| PUT | `/me/ratings` | Crear/actualizar valoración (0.5–5.0) |
| DELETE | `/me/ratings` | Eliminar valoración |
| GET | `/me/diary` | Mi diario de valoraciones |
| GET | `/users/{id}/diary` | Diario público de un usuario |
| GET | `/feed` | Feed de actividad (seguidos + yo, límite 50) |
| GET | `/me/following` | IDs que sigo |
| POST | `/me/following/{id}` | Seguir usuario |
| DELETE | `/me/following/{id}` | Dejar de seguir |
| GET | `/me/following/{id}` | Comprobar si sigo |
| GET | `/me/lists` | Mis listas |
| POST | `/me/lists` | Crear lista |
| DELETE | `/me/lists/{id}` | Eliminar lista |
| GET | `/lists/{id}/items` | Items (público o propio) |
| POST | `/me/lists/{id}/items` | Añadir item |
| DELETE | `/me/lists/{id}/items` | Quitar item |

### Uploads MinIO (Fase 2d)
| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/me/avatar` | Subir avatar (JPEG/PNG/WebP, máx 5 MB) |
| POST | `/catalog/artists/{id}/image` | Imagen de artista (solo autor del envío) |
| POST | `/catalog/albums/{id}/cover` | Portada de álbum (solo autor del envío) |
| POST | `/catalog/tracks/{id}/cover` | Portada de track (solo autor del envío) |
| POST | `/me/lists/{id}/cover` | Portada de lista (solo dueño) |

Buckets MinIO (públicos lectura): `avatars`, `catalog-covers`, `artist-images`, `list-covers`  
Consola MinIO: **http://localhost:9001** (`minioadmin` / `minioadmin`)

## Android

La app Android ya consume esta API vía Ktor + JWT. En `local.properties`:

```properties
API_BASE_URL=http://10.0.2.2:8000
```

Ver [`../README.md`](../README.md) para instrucciones completas.
