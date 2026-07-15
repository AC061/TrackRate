package com.example.trackrate.util

object PasswordValidator {
    const val MIN_LENGTH = 12

    private val HAS_LETTER = Regex("[A-Za-z]")
    private val HAS_DIGIT = Regex("\\d")
    private val HAS_SPECIAL = Regex("[^A-Za-z0-9]")

    fun validate(password: String): String? {
        if (password.length < MIN_LENGTH) {
            return "La contraseña debe tener al menos 12 caracteres"
        }
        if (!HAS_LETTER.containsMatchIn(password)) {
            return "La contraseña debe incluir al menos una letra"
        }
        if (!HAS_DIGIT.containsMatchIn(password)) {
            return "La contraseña debe incluir al menos un número"
        }
        if (!HAS_SPECIAL.containsMatchIn(password)) {
            return "La contraseña debe incluir al menos un carácter especial"
        }
        return null
    }
}
