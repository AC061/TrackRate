from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.deps.auth import get_current_user, require_admin
from app.models import User
from app.schemas.auth import ProfileResponse
from app.schemas.catalog import CatalogDetailResponse, CatalogSubmissionResponse, ModerationActionRequest, SetAdminRequest
from app.schemas.mappers import to_profile_response
from app.services import catalog_service, moderation_service

router = APIRouter(tags=["moderation", "admin"])


@router.get("/admin/moderation/pending", response_model=list[CatalogSubmissionResponse])
def pending_submissions(
    _: User = Depends(require_admin),
    db: Session = Depends(get_db),
) -> list[CatalogSubmissionResponse]:
    return moderation_service.list_pending_submissions(db)


@router.get(
    "/admin/moderation/{entity_type}/{entity_id}",
    response_model=CatalogDetailResponse,
)
def moderation_detail(
    entity_type: str,
    entity_id: UUID,
    _: User = Depends(require_admin),
    db: Session = Depends(get_db),
) -> CatalogDetailResponse:
    if entity_type not in ("artist", "album", "track"):
        raise HTTPException(400, "Tipo de entidad no válido")
    try:
        return catalog_service.get_submission_detail(db, entity_type, entity_id)
    except catalog_service.CatalogNotFoundError as exc:
        raise HTTPException(404, str(exc)) from exc
    except catalog_service.CatalogValidationError as exc:
        raise HTTPException(400, str(exc)) from exc


@router.patch(
    "/admin/moderation/{entity_type}/{entity_id}",
    response_model=CatalogSubmissionResponse,
)
def moderate_submission(
    entity_type: str,
    entity_id: UUID,
    body: ModerationActionRequest,
    admin: User = Depends(require_admin),
    db: Session = Depends(get_db),
) -> CatalogSubmissionResponse:
    if entity_type not in ("artist", "album", "track"):
        raise HTTPException(400, "Tipo de entidad no válido")
    try:
        return moderation_service.moderate_entity(
            db,
            admin.id,
            entity_type,
            entity_id,
            body.action,
            body.rejection_reason,
        )
    except moderation_service.EntityNotFoundError as exc:
        raise HTTPException(404, str(exc)) from exc
    except moderation_service.ModerationError as exc:
        raise HTTPException(400, str(exc)) from exc


@router.get("/me/submissions", response_model=list[CatalogSubmissionResponse])
def my_submissions(
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> list[CatalogSubmissionResponse]:
    return moderation_service.list_user_submissions(db, user.id)


@router.patch("/admin/users/{username}", response_model=ProfileResponse)
def set_user_admin(
    username: str,
    body: SetAdminRequest,
    _: User = Depends(require_admin),
    db: Session = Depends(get_db),
) -> ProfileResponse:
    try:
        profile = moderation_service.set_user_admin(db, username, body.make_admin)
    except moderation_service.EntityNotFoundError as exc:
        raise HTTPException(404, str(exc)) from exc
    return to_profile_response(profile)
