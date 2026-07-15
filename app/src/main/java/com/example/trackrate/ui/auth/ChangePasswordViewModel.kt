package com.example.trackrate.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackrate.data.repository.AuthRepository
import com.example.trackrate.util.PasswordValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordUiState(
    val isSaving: Boolean = false,
    val message: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        val validationError = validate(currentPassword, newPassword, confirmPassword)
        if (validationError != null) {
            _uiState.value = _uiState.value.copy(message = validationError, success = false)
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true, message = null, success = false)
        viewModelScope.launch {
            try {
                authRepository.changePassword(currentPassword, newPassword, confirmPassword)
                _uiState.value = ChangePasswordUiState(success = true, message = "Contraseña actualizada")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    message = e.message ?: "No se pudo cambiar la contraseña",
                    success = false
                )
            }
        }
    }

    fun consumeMessage() {
        if (_uiState.value.message != null) {
            _uiState.value = _uiState.value.copy(message = null)
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
        PasswordValidator.validate(newPassword)?.let { return it }
        if (newPassword != confirmPassword) {
            return "Las contraseñas no coinciden"
        }
        if (newPassword == currentPassword) {
            return "La nueva contraseña debe ser diferente a la actual"
        }
        return null
    }
}
