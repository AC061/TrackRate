package com.example.trackrate.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackrate.data.remote.ApiException
import com.example.trackrate.data.repository.AuthRepository
import com.example.trackrate.util.PasswordPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordUiState(
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(ChangePasswordUiState())

    val uiState: StateFlow<ChangePasswordUiState> =
        _uiState.asStateFlow()

    fun submit(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        val validationError = validate(
            currentPassword = currentPassword,
            newPassword = newPassword,
            confirmPassword = confirmPassword
        )

        if (validationError != null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = validationError,
                successMessage = null
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isSubmitting = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            try {
                val message = authRepository.changePassword(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    confirmPassword = confirmPassword
                )

                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    successMessage = message
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = mapError(error)
                )
            }
        }
    }

    fun consumeError() {
        if (_uiState.value.errorMessage != null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = null
            )
        }
    }

    fun consumeSuccess() {
        if (_uiState.value.successMessage != null) {
            _uiState.value = _uiState.value.copy(
                successMessage = null
            )
        }
    }

    private fun validate(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): String? {
        if (currentPassword.isBlank()) {
            return "Introduce tu contraseña actual"
        }

        if (newPassword.isBlank()) {
            return "Introduce una nueva contraseña"
        }

        PasswordPolicy.validationError(newPassword)?.let { error ->
            return error
        }

        if (confirmPassword.isBlank()) {
            return "Confirma la nueva contraseña"
        }

        if (newPassword != confirmPassword) {
            return "Las contraseñas nuevas no coinciden"
        }

        if (newPassword == currentPassword) {
            return "La nueva contraseña debe ser diferente de la actual"
        }

        return null
    }

    private fun mapError(error: Exception): String {
        if (error is ApiException) {
            return when (error.statusCode) {
                HttpStatusCode.BadRequest.value ->
                    error.message.ifBlank {
                        "La contraseña actual es incorrecta"
                    }

                HttpStatusCode.Unauthorized.value ->
                    "Tu sesión no es válida. Inicia sesión nuevamente."

                422 ->
                    error.message.ifBlank {
                        "La nueva contraseña no cumple los requisitos"
                    }

                else ->
                    error.message.ifBlank {
                        "No se pudo cambiar la contraseña"
                    }
            }
        }

        val messages = buildList {
            var current: Throwable? = error

            while (current != null) {
                current.message?.let { add(it) }
                current = current.cause
            }
        }.joinToString(" ")

        return when {
            messages.contains(
                "Connect timeout",
                ignoreCase = true
            ) ||
                messages.contains(
                    "Unable to resolve host",
                    ignoreCase = true
                ) ||
                messages.contains(
                    "Network is unreachable",
                    ignoreCase = true
                ) ||
                messages.contains(
                    "Failed to connect",
                    ignoreCase = true
                ) ->
                "No se pudo conectar con el servidor."

            messages.isNotBlank() ->
                messages

            else ->
                "No se pudo cambiar la contraseña"
        }
    }
}
