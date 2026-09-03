from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, EmailStr, Field, field_validator, model_validator

from app.core.password_policy import validate_password


class RegisterRequest(BaseModel):
    email: EmailStr
    password: str = Field(max_length=128)

    @field_validator("password")
    @classmethod
    def password_strength(cls, value: str) -> str:
        validate_password(value)
        return value


class LoginRequest(BaseModel):
    identifier: str = Field(min_length=1, max_length=320)
    password: str


class ChangePasswordRequest(BaseModel):
    current_password: str
    new_password: str = Field(max_length=128)
    confirm_password: str

    @field_validator("new_password")
    @classmethod
    def new_password_strength(cls, value: str) -> str:
        validate_password(value)
        return value

    @model_validator(mode="after")
    def passwords_match(self) -> "ChangePasswordRequest":
        if self.new_password != self.confirm_password:
            raise ValueError("Las contraseñas no coinciden")
        if self.new_password == self.current_password:
            raise ValueError("La nueva contraseña debe ser diferente a la actual")
        return self


class ProfileResponse(BaseModel):
    id: UUID
    username: str
    first_name: str
    last_name: str
    display_name: str | None
    bio: str | None
    avatar_url: str | None
    is_admin: bool
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class ProfileUpdateRequest(BaseModel):
    username: str = Field(min_length=3, max_length=30, pattern=r"^[a-z0-9_]{3,30}$")
    first_name: str = Field(min_length=1, max_length=100)
    last_name: str = Field(min_length=1, max_length=100)
    display_name: str | None = Field(default=None, max_length=100)
    bio: str | None = Field(default=None, max_length=2000)


class UserResponse(BaseModel):
    id: UUID
    email: EmailStr
    profile: ProfileResponse


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserResponse


class MessageResponse(BaseModel):
    detail: str
