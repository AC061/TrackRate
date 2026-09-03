import re

MIN_PASSWORD_LENGTH = 12


def validate_password(password: str) -> None:
    if len(password) < MIN_PASSWORD_LENGTH:
        raise ValueError(
            f"La contraseña debe tener al menos {MIN_PASSWORD_LENGTH} caracteres"
        )
    if not re.search(r"[A-Za-z]", password):
        raise ValueError("La contraseña debe incluir al menos una letra")
    if not re.search(r"\d", password):
        raise ValueError("La contraseña debe incluir al menos un número")
    if not re.search(r"[^A-Za-z0-9]", password):
        raise ValueError("La contraseña debe incluir al menos un carácter especial")
