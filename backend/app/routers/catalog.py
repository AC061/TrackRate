from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.deps.auth import get_current_user_with_profile
from app.models import User
from app.schemas.catalog import (
    CatalogDetailResponse,
    CatalogItemResponse,
    SubmitAlbumRequest,
    SubmitArtistRequest,
    SubmitTrackRequest,
    TopRatedTrackResponse,
)
from app.schemas.social import RatingStatsResponse
from app.services import catalog_service, rating_service

router = APIRouter(prefix="/catalog", tags=["catalog"])


@router.get("/search", response_model=list[CatalogItemResponse])
def search_catalog(
    q: str = Query(default="", max_length=200),
    type: str | None = Query(default=None, pattern=r"^(artist|album|track)$"),
    db: Session = Depends(get_db),
) -> list[CatalogItemResponse]:
    return catalog_service.search_catalog(db, q, type)


@router.get("/top-rated/tracks", response_model=list[TopRatedTrackResponse])
def top_rated_tracks(
    limit: int = Query(default=20, ge=1, le=50),
    db: Session = Depends(get_db),
) -> list[TopRatedTrackResponse]:
    return catalog_service.top_rated_tracks(db, limit)


@router.get("/artists/{artist_id}/albums", response_model=list[CatalogItemResponse])
def albums_by_artist(
    artist_id: UUID,
    db: Session = Depends(get_db),
) -> list[CatalogItemResponse]:
    return catalog_service.list_albums_by_artist(db, artist_id)


@router.get("/{entity_type}/{entity_id}/rating-stats", response_model=RatingStatsResponse | None)
def catalog_rating_stats(
    entity_type: str,
    entity_id: UUID,
    db: Session = Depends(get_db),
) -> RatingStatsResponse | None:
    if entity_type not in ("artist", "album", "track"):
        raise HTTPException(400, "Tipo de entidad no válido")
    return rating_service.get_entity_stats(db, entity_type, entity_id)


@router.get("/{entity_type}/{entity_id}", response_model=CatalogDetailResponse)
def catalog_detail(
    entity_type: str,
    entity_id: UUID,
    db: Session = Depends(get_db),
) -> CatalogDetailResponse:
    try:
        return catalog_service.get_catalog_detail(db, entity_type, entity_id)
    except catalog_service.CatalogNotFoundError as exc:
        raise HTTPException(404, str(exc)) from exc
    except catalog_service.CatalogValidationError as exc:
        raise HTTPException(400, str(exc)) from exc


@router.post("/artists", response_model=CatalogDetailResponse, status_code=201)
def submit_artist(
    body: SubmitArtistRequest,
    user: User = Depends(get_current_user_with_profile),
    db: Session = Depends(get_db),
) -> CatalogDetailResponse:
    artist = catalog_service.submit_artist(db, user.id, body.name, body.bio)
    return catalog_service.build_artist_detail_response(db, artist)


@router.post("/albums", response_model=CatalogDetailResponse, status_code=201)
def submit_album(
    body: SubmitAlbumRequest,
    user: User = Depends(get_current_user_with_profile),
    db: Session = Depends(get_db),
) -> CatalogDetailResponse:
    try:
        album = catalog_service.submit_album(
            db,
            user.id,
            body.title,
            body.artist_id,
            body.release_year,
            body.description,
            body.label_id,
            body.contributors,
        )
    except catalog_service.CatalogValidationError as exc:
        raise HTTPException(400, str(exc)) from exc
    return catalog_service.build_album_detail_response(db, album)


@router.post("/tracks", response_model=CatalogDetailResponse, status_code=201)
def submit_track(
    body: SubmitTrackRequest,
    user: User = Depends(get_current_user_with_profile),
    db: Session = Depends(get_db),
) -> CatalogDetailResponse:
    try:
        track = catalog_service.submit_track(
            db,
            user.id,
            body.title,
            body.artist_id,
            body.album_id,
            body.duration_ms,
            body.description,
            body.label_id,
            body.contributors,
            body.samples,
        )
    except catalog_service.CatalogValidationError as exc:
        raise HTTPException(400, str(exc)) from exc
    return catalog_service.build_track_detail_response(db, track)
