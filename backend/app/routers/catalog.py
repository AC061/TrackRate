from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.schemas.catalog import CatalogDetailResponse, CatalogItemResponse, CoverResponse
from app.services import catalog_service

router = APIRouter(prefix="/catalog", tags=["catalog"])


@router.get("/search", response_model=list[CatalogItemResponse])
def search_catalog(
    q: str = Query(default="", max_length=200),
    type: str | None = Query(default=None, pattern=r"^(artist|album|track)$"),
    db: Session = Depends(get_db),
) -> list[CatalogItemResponse]:
    return catalog_service.search_catalog(db, q, type)


@router.get("/artists/{artist_id}/albums", response_model=list[CatalogItemResponse])
def albums_by_artist(
    artist_id: UUID,
    db: Session = Depends(get_db),
) -> list[CatalogItemResponse]:
    try:
        return catalog_service.list_albums_by_artist(db, artist_id)
    except catalog_service.CatalogNotFoundError as exc:
        raise HTTPException(404, str(exc)) from exc


@router.get("/{entity_type}/{entity_id}/cover", response_model=CoverResponse)
def catalog_cover(
    entity_type: str,
    entity_id: UUID,
) -> CoverResponse:
    try:
        url = catalog_service.get_cover_url(entity_type, entity_id)
    except catalog_service.CatalogValidationError as exc:
        raise HTTPException(400, str(exc)) from exc
    return CoverResponse(url=url)


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
