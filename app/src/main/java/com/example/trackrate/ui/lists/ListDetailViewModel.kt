package com.example.trackrate.ui.lists

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackrate.data.repository.ListRepository
import com.example.trackrate.data.repository.UploadRepository
import com.example.trackrate.domain.model.ListItemDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListDetailUiState(
    val isLoading: Boolean = true,
    val listTitle: String = "",
    val coverUrl: String? = null,
    val items: List<ListItemDetail> = emptyList(),
    val isUploadingCover: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class ListDetailViewModel @Inject constructor(
    private val listRepository: ListRepository,
    private val uploadRepository: UploadRepository
) : ViewModel() {

    private var listId: String? = null
    private var listTitle: String = ""

    private val _uiState = MutableStateFlow(ListDetailUiState())
    val uiState: StateFlow<ListDetailUiState> = _uiState.asStateFlow()

    fun init(listId: String, listTitle: String) {
        this.listId = listId
        this.listTitle = listTitle
        load()
    }

    fun load() {
        val id = listId ?: return
        _uiState.value = _uiState.value.copy(isLoading = true, message = null, listTitle = listTitle)
        viewModelScope.launch {
            try {
                val items = listRepository.getListItems(id)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    listTitle = listTitle,
                    items = items
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    listTitle = listTitle,
                    message = e.message ?: "Error al cargar la lista"
                )
            }
        }
    }

    fun uploadCover(uri: Uri) {
        val id = listId ?: return
        _uiState.value = _uiState.value.copy(isUploadingCover = true, message = null)
        viewModelScope.launch {
            try {
                val payload = uploadRepository.readImage(uri)
                val url = uploadRepository.uploadListCover(id, payload)
                _uiState.value = _uiState.value.copy(
                    isUploadingCover = false,
                    coverUrl = url,
                    message = "Portada actualizada"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploadingCover = false,
                    message = e.message ?: "No se pudo subir la portada"
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
