package com.example.trackrate.util

import com.example.trackrate.data.remote.ApiException
import io.ktor.http.HttpStatusCode

object ApiErrorMapper {

    data class MappedError(
        val message: String,
        val isNetworkError: Boolean = false,
        val isUnauthorized: Boolean = false
    )

    fun map(throwable: Throwable): MappedError {
        if (throwable is ApiException) {
            return when (throwable.statusCode) {
                HttpStatusCode.Unauthorized.value -> MappedError(
                    message = "Tu sesión expiró. Inicia sesión de nuevo.",
                    isUnauthorized = true
                )
                HttpStatusCode.Forbidden.value -> MappedError(
                    message = throwable.message.ifBlank { "No tienes permiso para esta acción." }
                )
                HttpStatusCode.NotFound.value -> MappedError(
                    message = throwable.message.ifBlank { "Recurso no encontrado." }
                )
                in 500..599 -> MappedError(
                    message = "Error del servidor. Inténtalo más tarde."
                )
                else -> MappedError(
                    message = throwable.message.ifBlank { "Error de la aplicación." }
                )
            }
        }

        val combined = buildList {
            var current: Throwable? = throwable
            while (current != null) {
                current.message?.let { add(it) }
                current = current.cause
            }
        }.joinToString(" ")

        return when {
            combined.contains("Connect timeout", ignoreCase = true) ||
                combined.contains("Unable to resolve host", ignoreCase = true) ||
                combined.contains("Network is unreachable", ignoreCase = true) ||
                combined.contains("Failed to connect", ignoreCase = true) ||
                combined.contains("Software caused connection abort", ignoreCase = true) ->
                MappedError(
                    message = "Sin conexión con el servidor. Comprueba tu red e inténtalo de nuevo.",
                    isNetworkError = true
                )
            combined.isNotBlank() -> MappedError(message = combined)
            else -> MappedError(message = "Ocurrió un error inesperado.")
        }
    }
}
