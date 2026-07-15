# Supabase (archivado)

> **Este directorio ya no se usa.** TrackRate migró a FastAPI + PostgreSQL + MinIO.
> Se conserva solo como referencia histórica del esquema, RLS y triggers originales.

El backend activo está en [`../../backend/`](../../backend/).

## Qué reemplazó cada pieza

| Supabase | Backend actual |
|----------|----------------|
| `auth.users` + Auth SDK | `users` + JWT (`POST /auth/login`, `/auth/register`) |
| PostgREST | FastAPI REST (`/catalog`, `/me/ratings`, `/feed`, …) |
| Storage buckets | MinIO (`POST /me/avatar`, `/catalog/.../cover`, …) |
| RLS + triggers | Services Python + constraints PostgreSQL |
| Vistas `approved_*`, `rating_details`, etc. | Queries en services |

## Migraciones originales

Los archivos en `migrations/` documentan el diseño inicial del dominio. El esquema vigente
está en `backend/alembic/versions/001_initial_schema.py`.

> **Nota:** La carpeta `.temp/` (metadatos locales del CLI Supabase) no debe versionarse.
> Está listada en `.gitignore`.
