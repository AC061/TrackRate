package com.example.trackrate.ui.moderation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackrate.data.repository.ModerationRepository
import com.example.trackrate.domain.model.CatalogSubmission
import com.example.trackrate.domain.model.CatalogType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModerationUiState(
    val isLoading: Boolean = true,
    val items: List<CatalogSubmission> = emptyList(),
    val message: String? = null
)

@HiltViewModel
class ModerationViewModel @Inject constructor(
    private val moderationRepository: ModerationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModerationUiState())
    val uiState: StateFlow<ModerationUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true, message = null)
        viewModelScope.launch {
            try {
                val items = moderationRepository.getPending()
                _uiState.value = ModerationUiState(isLoading = false, items = items)
            } catch (e: Exception) {
                _uiState.value = ModerationUiState(
                    isLoading = false,
                    message = e.message ?: "Error al cargar la cola"
                )
            }
        }
    }

    fun approve(type: CatalogType, id: String) {
        viewModelScope.launch {
            try {
                moderationRepository.approve(type, id)
                _uiState.value = _uiState.value.copy(message = "Aprobado")
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = e.message ?: "No se pudo aprobar")
            }
        }
    }

    fun reject(type: CatalogType, id: String, reason: String) {
        viewModelScope.launch {
            try {
                moderationRepository.reject(type, id, reason)
                _uiState.value = _uiState.value.copy(message = "Rechazado")
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = e.message ?: "No se pudo rechazar")
            }
        }
    }

    fun consumeMessage() {
        if (_uiState.value.message != null) {
            _uiState.value = _uiState.value.copy(message = null)
        }
    }
}
