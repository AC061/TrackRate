package com.example.trackrate.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackrate.data.repository.CatalogRepository
import com.example.trackrate.domain.model.CatalogItem
import com.example.trackrate.domain.model.CatalogType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val isLoading: Boolean = false,
    val items: List<CatalogItem> = emptyList(),
    val selectedType: CatalogType? = null,
    val message: String? = null,
    val hasSearched: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var currentQuery: String = ""
    private var searchJob: Job? = null

    init {
        runSearch(immediate = true)
    }

    fun onQueryChanged(query: String) {
        currentQuery = query
        runSearch(immediate = false)
    }

    fun onTypeSelected(type: CatalogType?) {
        _uiState.value = _uiState.value.copy(selectedType = type)
        runSearch(immediate = true)
    }

    fun refresh() = runSearch(immediate = true)

    private fun runSearch(immediate: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (!immediate) delay(DEBOUNCE_MS)
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            try {
                val items = catalogRepository.search(currentQuery, _uiState.value.selectedType)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    items = items,
                    hasSearched = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = e.message ?: "Error al buscar"
                )
            }
        }
    }

    fun consumeMessage() {
        if (_uiState.value.message != null) {
            _uiState.value = _uiState.value.copy(message = null)
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 350L
    }
}
