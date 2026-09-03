from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.schemas.catalog import TopRatedEntityResponse
from app.schemas.social import RatingStatsResponse
from app.services import catalog_service, rating_service

router = APIRouter(prefix="/entities", tags=["entities"])


@router.get("/top-rated", response_model=list[TopRatedEntityResponse])
def top_rated(
    type: str = Query(default="track", pattern=r"^(artist|album|track)$"),
    limit: int = Query(default=20, ge=1, le=50),
    db: Session = Depends(get_db),
) -> list[TopRatedEntityResponse]:
    try:
        return catalog_service.top_rated_entities(db, type, limit)
    except catalog_service.CatalogValidationError as exc:
        raise HTTPException(400, str(exc)) from exc


@router.get("/{entity_type}/{entity_id}/rating-stats", response_model=RatingStatsResponse | None)
def entity_rating_stats(
    entity_type: str,
    entity_id: UUID,
    db: Session = Depends(get_db),
) -> RatingStatsResponse | None:
    if entity_type not in ("artist", "album", "track"):
        raise HTTPException(400, "Tipo de entidad no válido")
    return rating_service.get_entity_stats(db, entity_type, entity_id)
