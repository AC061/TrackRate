package com.example.trackrate.ui.submit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackrate.data.repository.CatalogRepository
import com.example.trackrate.data.repository.UploadRepository
import com.example.trackrate.domain.model.CatalogItem
import com.example.trackrate.domain.model.CatalogType
import com.example.trackrate.util.ImagePayload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubmitUiState(
    val type: CatalogType = CatalogType.ARTIST,
    val artists: List<CatalogItem> = emptyList(),
    val albums: List<CatalogItem> = emptyList(),
    val isSubmitting: Boolean = false,
    val message: String? = null
)

data class SubmitResult(
    val type: CatalogType,
    val entityId: String
)

@HiltViewModel
class SubmitViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val uploadRepository: UploadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubmitUiState())
    val uiState: StateFlow<SubmitUiState> = _uiState.asStateFlow()

    private val _submitted = MutableSharedFlow<SubmitResult>()
    val submitted: SharedFlow<SubmitResult> = _submitted.asSharedFlow()

    init {
        loadArtists()
    }

    fun setType(type: CatalogType) {
        _uiState.value = _uiState.value.copy(type = type)
    }

    private fun loadArtists() {
        viewModelScope.launch {
            try {
                val artists = catalogRepository.getApprovedArtists()
                _uiState.value = _uiState.value.copy(artists = artists)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = e.message ?: "No se pudieron cargar los artistas"
                )
            }
        }
    }

    fun onArtistSelected(artistId: String) {
        viewModelScope.launch {
            try {
                val albums = catalogRepository.getApprovedAlbumsByArtist(artistId)
                _uiState.value = _uiState.value.copy(albums = albums)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = e.message ?: "No se pudieron cargar los álbumes"
                )
            }
        }
    }

    fun submitArtist(name: String, bio: String) {
        if (name.isBlank()) {
            emitMessage("El nombre del artista es obligatorio")
            return
        }
        runSubmit(CatalogType.ARTIST) {
            catalogRepository.submitArtist(name, bio)
        }
    }

    fun submitAlbum(title: String, artistId: String?, releaseYear: Int?) {
        if (title.isBlank()) {
            emitMessage("El título del álbum es obligatorio")
            return
        }
        if (artistId == null) {
            emitMessage("Selecciona un artista aprobado")
            return
        }
        runSubmit(CatalogType.ALBUM) {
            catalogRepository.submitAlbum(title, artistId, releaseYear)
        }
    }

    fun submitTrack(title: String, artistId: String?, albumId: String?, durationMs: Int?) {
        if (title.isBlank()) {
            emitMessage("El título de la canción es obligatorio")
            return
        }
        if (artistId == null) {
            emitMessage("Selecciona un artista aprobado")
            return
        }
        runSubmit(CatalogType.TRACK) {
            catalogRepository.submitTrack(title, artistId, albumId, durationMs)
        }
    }

    suspend fun uploadImage(type: CatalogType, entityId: String, payload: ImagePayload) {
        uploadRepository.uploadCatalogImage(type, entityId, payload)
    }

    private fun runSubmit(type: CatalogType, action: suspend () -> String) {
        _uiState.value = _uiState.value.copy(isSubmitting = true, message = null)
        viewModelScope.launch {
            try {
                val entityId = action()
                _uiState.value = _uiState.value.copy(isSubmitting = false)
                _submitted.emit(SubmitResult(type, entityId))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    message = e.message ?: "No se pudo enviar"
                )
            }
        }
    }

    private fun emitMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }

    fun consumeMessage() {
        if (_uiState.value.message != null) {
            _uiState.value = _uiState.value.copy(message = null)
        }
    }
}
