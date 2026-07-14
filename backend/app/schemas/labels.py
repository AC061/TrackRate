from uuid import UUID

from pydantic import BaseModel, Field


class RecordLabelResponse(BaseModel):
    id: UUID
    name: str


class CreateRecordLabelRequest(BaseModel):
    name: str = Field(min_length=1, max_length=200)
