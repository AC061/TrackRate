package com.example.trackrate.ui.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackrate.data.repository.ListRepository
import com.example.trackrate.domain.model.MusicList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListsUiState(
    val isLoading: Boolean = true,
    val lists: List<MusicList> = emptyList(),
    val message: String? = null
)

@HiltViewModel
class ListsViewModel @Inject constructor(
    private val listRepository: ListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListsUiState())
    val uiState: StateFlow<ListsUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true, message = null)
        viewModelScope.launch {
            try {
                val lists = listRepository.getMyLists()
                _uiState.value = ListsUiState(isLoading = false, lists = lists)
            } catch (e: Exception) {
                _uiState.value = ListsUiState(
                    isLoading = false,
                    message = e.message ?: "Error al cargar listas"
                )
            }
        }
    }

    fun createList(title: String, description: String?, isPublic: Boolean) {
        viewModelScope.launch {
            try {
                listRepository.createList(title, description, isPublic)
                _uiState.value = _uiState.value.copy(message = "Lista creada")
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = e.message ?: "No se pudo crear la lista")
            }
        }
    }

    fun consumeMessage() {
        if (_uiState.value.message != null) {
            _uiState.value = _uiState.value.copy(message = null)
        }
    }
}
