# Licencias y atribución — TrackRate

Documentación de recursos de terceros y software utilizado en el proyecto (requisito **II. Multimedia / licencias** del checklist 1GS132).

## Recursos propios del equipo

| Recurso | Ubicación | Autoría | Licencia |
|---------|-----------|---------|----------|
| Logotipo TrackRate (icono y wordmark) | `logo_icon.svg`, `logo_full.svg`, `app/src/main/res/drawable/logo_*.xml` | Integrantes del equipo (ver ENT-1 en `MainActivity.kt`) | Uso exclusivo del proyecto académico |
| Capturas / imágenes subidas por usuarios | MinIO (`avatars`, `catalog-covers`, …) | Usuario que las sube | Responsabilidad del usuario; la app no redistribuye catálogo de terceros |
| Datos de catálogo de prueba (seed dev) | `backend/scripts/seed.py` | Metadatos ficticios para desarrollo | Solo uso local / demostración |

## Iconografía — Material Design Icons (Pictogrammers)

| Recurso | Ubicación | Licencia |
|---------|-----------|----------|
| Iconos MDI (home, magnify, star, album, …) | `app/src/main/res/drawable/ic_mdi_*.xml` | [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) |

Fuente: [Material Design Icons — Pictogrammers](https://pictogrammers.com/library/mdi/)  
Convención interna: [`docs/icons.md`](icons.md)

## Dependencias Android (app)

Todas bajo licencias permisivas salvo indicación contraria. Versiones en `gradle/libs.versions.toml`.

| Biblioteca | Uso en TrackRate | Licencia |
|------------|------------------|----------|
| AndroidX (Core, AppCompat, Lifecycle, Navigation, Room, RecyclerView, ConstraintLayout) | UI, navegación, caché offline | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| Material Components for Android | Componentes Material (botones, campos, toolbar) | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| Kotlin / Kotlinx Coroutines / Kotlinx Serialization | Lenguaje y concurrencia | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| Dagger Hilt | Inyección de dependencias | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| Ktor Client | Cliente HTTP hacia la API | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| Coil | Carga de imágenes (avatares, portadas) | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| JUnit / Espresso | Pruebas | [EPL 2.0](https://www.eclipse.org/legal/epl-2.0/) |

## Dependencias backend (`backend/requirements.txt`)

| Paquete | Uso | Licencia |
|---------|-----|----------|
| FastAPI / Starlette / Uvicorn | API REST | [MIT](https://opensource.org/licenses/MIT) |
| SQLAlchemy / Alembic | ORM y migraciones | [MIT](https://opensource.org/licenses/MIT) |
| Pydantic / pydantic-settings | Validación de datos | [MIT](https://opensource.org/licenses/MIT) |
| psycopg | Driver PostgreSQL | [LGPL-3.0](https://www.gnu.org/licenses/lgpl-3.0.html) |
| python-jose | JWT | [MIT](https://opensource.org/licenses/MIT) |
| bcrypt | Hash de contraseñas | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| python-multipart | Uploads multipart | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| email-validator | Validación de email | [Unlicense](https://unlicense.org/) |
| MinIO Python SDK | Cliente S3/MinIO | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |

## Infraestructura (Docker)

| Componente | Imagen / servicio | Licencia |
|------------|-------------------|----------|
| PostgreSQL | Imagen oficial Docker | [PostgreSQL License](https://www.postgresql.org/about/licence/) (similar a BSD/MIT) |
| MinIO | Almacenamiento de objetos | [GNU AGPL v3](https://www.gnu.org/licenses/agpl-3.0.html) — solo en entorno de desarrollo local vía Docker; no se redistribuye el binario dentro del APK |

## Imágenes en tiempo de ejecución

| Origen | Notas |
|--------|-------|
| MinIO (URLs públicas de buckets) | Avatares y portadas subidas por usuarios o administradores |
| Placeholders locales | Drawables MDI cuando no hay imagen remota |

La app **no** incorpora catálogo de imágenes con copyright de terceros (Spotify, etc.). El seed de desarrollo usa metadatos textuales; las portadas las aporta el usuario o quedan vacías.

## Referencia histórica Supabase

El directorio `docs/legacy-supabase/` conserva migraciones del prototipo inicial. **No forma parte del producto entregado**; el backend activo es FastAPI (`backend/`).
