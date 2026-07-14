package com.example.trackrate.ui.moderation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackrate.data.repository.ModerationRepository
import com.example.trackrate.domain.model.CatalogDetail
import com.example.trackrate.domain.model.CatalogType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModerationReviewUiState(
    val isLoading: Boolean = true,
    val detail: CatalogDetail? = null,
    val message: String? = null
)

@HiltViewModel
class ModerationReviewViewModel @Inject constructor(
    private val moderationRepository: ModerationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModerationReviewUiState())
    val uiState: StateFlow<ModerationReviewUiState> = _uiState.asStateFlow()

    private val _finished = MutableSharedFlow<Unit>()
    val finished: SharedFlow<Unit> = _finished.asSharedFlow()

    private var type: CatalogType? = null
    private var id: String? = null

    fun load(type: CatalogType, id: String) {
        this.type = type
        this.id = id
        _uiState.value = ModerationReviewUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val detail = moderationRepository.getSubmissionDetail(type, id)
                _uiState.value = ModerationReviewUiState(isLoading = false, detail = detail)
            } catch (e: Exception) {
                _uiState.value = ModerationReviewUiState(
                    isLoading = false,
                    message = e.message ?: "No se pudo cargar el envío"
                )
            }
        }
    }

    fun approve() {
        val currentType = type ?: return
        val currentId = id ?: return
        viewModelScope.launch {
            try {
                moderationRepository.approve(currentType, currentId)
                _finished.emit(Unit)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = e.message ?: "Error al aprobar")
            }
        }
    }

    fun reject(reason: String) {
        val currentType = type ?: return
        val currentId = id ?: return
        viewModelScope.launch {
            try {
                moderationRepository.reject(currentType, currentId, reason)
                _finished.emit(Unit)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = e.message ?: "Error al rechazar")
            }
        }
    }

    fun consumeMessage() {
        if (_uiState.value.message != null) {
            _uiState.value = _uiState.value.copy(message = null)
        }
    }
}
