from contextlib import asynccontextmanager
import logging
import time

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.routers import (
    auth,
    catalog,
    feed,
    follows,
    health,
    lists,
    moderation,
    profiles,
    ratings,
    uploads,
    users,
)
from app.services.storage_service import ensure_buckets

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(_: FastAPI):
    for attempt in range(10):
        try:
            ensure_buckets()
            logger.info("MinIO buckets listos")
            break
        except Exception as exc:
            if attempt == 9:
                logger.warning("MinIO no disponible al arrancar: %s", exc)
            else:
                time.sleep(2)
    yield


app = FastAPI(
    title="TrackRate API",
    version="0.4.0",
    description="Backend REST para TrackRate (migración desde Supabase)",
    lifespan=lifespan,
)
origins = [o.strip() for o in settings.cors_origins.split(",") if o.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins if origins != ["*"] else ["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health.router)
app.include_router(auth.router)
app.include_router(profiles.router)
app.include_router(catalog.router)
app.include_router(moderation.router)
app.include_router(ratings.router)
app.include_router(feed.router)
app.include_router(follows.router)
app.include_router(lists.router)
app.include_router(users.router)
app.include_router(uploads.router)
