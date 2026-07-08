package com.example.trackrate.ui.moderation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackrate.data.repository.ModerationRepository
import com.example.trackrate.data.repository.UploadRepository
import com.example.trackrate.domain.model.CatalogSubmission
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubmissionsUiState(
    val isLoading: Boolean = true,
    val items: List<CatalogSubmission> = emptyList(),
    val isUploading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SubmissionsViewModel @Inject constructor(
    private val moderationRepository: ModerationRepository,
    private val uploadRepository: UploadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubmissionsUiState())
    val uiState: StateFlow<SubmissionsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true, message = null)
        viewModelScope.launch {
            try {
                val items = moderationRepository.getMySubmissions()
                _uiState.value = SubmissionsUiState(isLoading = false, items = items)
            } catch (e: Exception) {
                _uiState.value = SubmissionsUiState(
                    isLoading = false,
                    message = e.message ?: "Error al cargar tus envíos"
                )
            }
        }
    }

    fun uploadImage(item: CatalogSubmission, uri: Uri) {
        _uiState.value = _uiState.value.copy(isUploading = true, message = null)
        viewModelScope.launch {
            try {
                val payload = uploadRepository.readImage(uri)
                uploadRepository.uploadCatalogImage(item.type, item.id, payload)
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    message = "Imagen subida correctamente"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    message = e.message ?: "No se pudo subir la imagen"
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
