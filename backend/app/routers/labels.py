from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.deps.auth import require_admin
from app.models import User
from app.schemas.labels import CreateRecordLabelRequest, RecordLabelResponse
from app.services import label_service

router = APIRouter(tags=["labels"])


@router.get("/labels", response_model=list[RecordLabelResponse])
def list_record_labels(db: Session = Depends(get_db)) -> list[RecordLabelResponse]:
    return label_service.list_labels(db)


@router.post("/admin/labels", response_model=RecordLabelResponse, status_code=201)
def create_record_label(
    body: CreateRecordLabelRequest,
    _: User = Depends(require_admin),
    db: Session = Depends(get_db),
) -> RecordLabelResponse:
    try:
        return label_service.create_label(db, body.name)
    except label_service.LabelError as exc:
        raise HTTPException(400, str(exc)) from exc
