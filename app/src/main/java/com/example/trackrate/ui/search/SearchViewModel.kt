package com.example.trackrate.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackrate.data.repository.CatalogRepository
import com.example.trackrate.domain.model.CatalogItem
import com.example.trackrate.domain.model.CatalogType
import com.example.trackrate.util.ApiErrorMapper
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
    val loadError: String? = null,
    val canRetry: Boolean = false,
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

    fun retry() = runSearch(immediate = true)

    private fun runSearch(immediate: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (!immediate) delay(DEBOUNCE_MS)
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadError = null,
                canRetry = false,
                message = null
            )
            try {
                val items = catalogRepository.search(currentQuery, _uiState.value.selectedType)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    items = items,
                    hasSearched = true
                )
            } catch (e: Exception) {
                val mapped = ApiErrorMapper.map(e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadError = if (mapped.isUnauthorized) null else mapped.message,
                    canRetry = !mapped.isUnauthorized,
                    hasSearched = true
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
