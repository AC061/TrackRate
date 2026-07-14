package com.example.trackrate.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackrate.data.repository.CatalogRepository
import com.example.trackrate.data.repository.FeedRepository
import com.example.trackrate.domain.model.ActivityFeedItem
import com.example.trackrate.domain.model.TopRatedTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val topRated: List<TopRatedTrack> = emptyList(),
    val items: List<ActivityFeedItem> = emptyList(),
    val message: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true, message = null)
        viewModelScope.launch {
            try {
                val (topRated, feed) = coroutineScope {
                    val topDeferred = async { catalogRepository.getTopRatedTracks() }
                    val feedDeferred = async { feedRepository.getFeed() }
                    topDeferred.await() to feedDeferred.await()
                }
                _uiState.value = HomeUiState(
                    isLoading = false,
                    topRated = topRated,
                    items = feed
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    message = e.message ?: "Error al cargar el inicio"
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
