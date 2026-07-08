package com.example.trackrate.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackrate.data.repository.ModerationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUsersUiState(
    val isWorking: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class AdminUsersViewModel @Inject constructor(
    private val moderationRepository: ModerationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUsersUiState())
    val uiState: StateFlow<AdminUsersUiState> = _uiState.asStateFlow()

    fun setAdmin(username: String, makeAdmin: Boolean) {
        val trimmed = username.trim()
        if (!Regex("^[a-z0-9_]{3,30}$").matches(trimmed)) {
            _uiState.value = _uiState.value.copy(message = "Nombre de usuario no válido")
            return
        }

        _uiState.value = _uiState.value.copy(isWorking = true, message = null)
        viewModelScope.launch {
            try {
                moderationRepository.setUserAdmin(trimmed, makeAdmin)
                _uiState.value = AdminUsersUiState(
                    isWorking = false,
                    message = if (makeAdmin) "$trimmed ahora es administrador"
                    else "$trimmed ya no es administrador"
                )
            } catch (e: Exception) {
                _uiState.value = AdminUsersUiState(
                    isWorking = false,
                    message = e.message ?: "No se pudo actualizar el rol"
                )
            }
        }
    }

    fun consumeMessage() {
        if (_uiState.value.message != null) {
            _uiState.value = _uiState.value.copy(message = null)
        }
    }
}
