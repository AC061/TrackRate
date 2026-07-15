PASSWORD_ERROR_MESSAGE = (
    "La contraseña debe tener al menos 12 caracteres, "
    "una letra, un número y un carácter especial."
)


def validate_password_strength(password: str) -> str:
    """
    Valida las contraseñas nuevas según la rúbrica.

    Requisitos:
    - Mínimo 12 caracteres.
    - Al menos una letra.
    - Al menos un número.
    - Al menos un carácter especial.
    """

    has_minimum_length = len(password) >= 12
    has_letter = any(character.isalpha() for character in password)
    has_number = any(character.isdigit() for character in password)
    has_special_character = any(
        not character.isalnum() and not character.isspace()
        for character in password
    )

    is_valid = all(
        (
            has_minimum_length,
            has_letter,
            has_number,
            has_special_character,
        )
    )

    if not is_valid:
        raise ValueError(PASSWORD_ERROR_MESSAGE)

    return password
