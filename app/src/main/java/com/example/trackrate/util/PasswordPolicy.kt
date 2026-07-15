package com.example.trackrate.util

object PasswordPolicy {

    const val ERROR_MESSAGE =
        "La contraseña debe tener al menos 12 caracteres, una letra, un número y un carácter especial."

    fun validationError(password: String): String? {
        val isValid =
            password.length >= 12 &&
                    password.any { it.isLetter() } &&
                    password.any { it.isDigit() } &&
                    password.any {
                        !it.isLetterOrDigit() && !it.isWhitespace()
                    }

        return if (isValid) null else ERROR_MESSAGE
    }
}