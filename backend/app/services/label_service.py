from uuid import UUID

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models import RecordLabel
from app.schemas.labels import RecordLabelResponse


class LabelError(Exception):
    pass


class LabelNotFoundError(Exception):
    pass


def list_labels(db: Session) -> list[RecordLabelResponse]:
    labels = db.scalars(select(RecordLabel).order_by(RecordLabel.name)).all()
    return [RecordLabelResponse(id=label.id, name=label.name) for label in labels]


def create_label(db: Session, name: str) -> RecordLabelResponse:
    trimmed = name.strip()
    if not trimmed:
        raise LabelError("El nombre del sello es obligatorio")

    existing = db.scalar(select(RecordLabel).where(RecordLabel.name.ilike(trimmed)))
    if existing is not None:
        return RecordLabelResponse(id=existing.id, name=existing.name)

    label = RecordLabel(name=trimmed)
    db.add(label)
    db.commit()
    db.refresh(label)
    return RecordLabelResponse(id=label.id, name=label.name)


def get_label_name(db: Session, label_id: UUID | None) -> str | None:
    if label_id is None:
        return None
    return db.scalar(select(RecordLabel.name).where(RecordLabel.id == label_id))


def validate_label_id(db: Session, label_id: UUID | None) -> None:
    if label_id is None:
        return
    label = db.get(RecordLabel, label_id)
    if label is None:
        raise LabelNotFoundError("Sello discográfico no encontrado")
