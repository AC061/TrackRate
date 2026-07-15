package com.example.trackrate.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackrate.data.remote.ApiException
import com.example.trackrate.data.repository.AuthRepository
import com.example.trackrate.domain.model.SessionStatus
import com.example.trackrate.util.PasswordValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthMode { LOGIN, REGISTER }

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface AuthEvent {
    data object LoginSuccess : AuthEvent
    data object RegisterSuccess : AuthEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    val sessionStatus: StateFlow<SessionStatus> = authRepository.sessionStatus

    fun bootstrap() {
        viewModelScope.launch {
            authRepository.bootstrap()
        }
    }

    fun toggleMode() {
        val newMode = if (_uiState.value.mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN
        _uiState.value = AuthUiState(mode = newMode)
    }

    fun submit(identifier: String, password: String, confirmPassword: String) {
        val validationError = validate(identifier, password, confirmPassword, _uiState.value.mode)
        if (validationError != null) {
            _uiState.value = _uiState.value.copy(errorMessage = validationError)
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                when (_uiState.value.mode) {
                    AuthMode.LOGIN -> {
                        authRepository.signIn(identifier, password)
                        _events.emit(AuthEvent.LoginSuccess)
                    }
                    AuthMode.REGISTER -> {
                        authRepository.signUp(identifier, password)
                        _events.emit(AuthEvent.RegisterSuccess)
                    }
                }
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = mapAuthError(e)
                )
            }
        }
    }

    fun clearError() {
        if (_uiState.value.errorMessage != null) {
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }

    private fun validate(
        identifier: String,
        password: String,
        confirmPassword: String,
        mode: AuthMode
    ): String? {
        val trimmedIdentifier = identifier.trim()
        when (mode) {
            AuthMode.LOGIN -> {
                if (trimmedIdentifier.isBlank()) {
                    return "Introduce tu usuario o correo"
                }
                if (password.isBlank()) {
                    return "Introduce tu contraseña"
                }
            }
            AuthMode.REGISTER -> {
                if (trimmedIdentifier.isBlank() ||
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedIdentifier).matches()
                ) {
                    return "Introduce un email válido"
                }
                PasswordValidator.validate(password)?.let { return it }
                if (password != confirmPassword) {
                    return "Las contraseñas no coinciden"
                }
            }
        }
        return null
    }

    private fun mapAuthError(e: Exception): String {
        if (e is ApiException) {
            return when (e.statusCode) {
                HttpStatusCode.Unauthorized.value ->
                    "Usuario o correo y contraseña incorrectos"
                HttpStatusCode.Conflict.value ->
                    "Este email ya está registrado"
                else -> e.message.ifBlank { "Error de autenticación" }
            }
        }

        val messages = buildList {
            var current: Throwable? = e
            while (current != null) {
                current.message?.let { add(it) }
                current = current.cause
            }
        }.joinToString(" ")

        return when {
            messages.contains("Connect timeout", ignoreCase = true) ||
                messages.contains("Unable to resolve host", ignoreCase = true) ||
                messages.contains("Network is unreachable", ignoreCase = true) ||
                messages.contains("Failed to connect", ignoreCase = true) ->
                "No se pudo conectar con el servidor. Comprueba que la API esté en marcha."
            messages.isNotBlank() -> messages
            else -> "Error de autenticación"
        }
    }
}
