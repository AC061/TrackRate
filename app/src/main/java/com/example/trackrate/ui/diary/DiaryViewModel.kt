package com.example.trackrate.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackrate.data.repository.RatingRepository
import com.example.trackrate.domain.model.DiaryEntry
import com.example.trackrate.util.ApiErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiaryUiState(
    val isLoading: Boolean = true,
    val entries: List<DiaryEntry> = emptyList(),
    val loadError: String? = null,
    val canRetry: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val ratingRepository: RatingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            loadError = null,
            canRetry = false,
            message = null
        )
        viewModelScope.launch {
            try {
                val entries = ratingRepository.getMyDiary()
                _uiState.value = DiaryUiState(isLoading = false, entries = entries)
            } catch (e: Exception) {
                val mapped = ApiErrorMapper.map(e)
                _uiState.value = DiaryUiState(
                    isLoading = false,
                    loadError = if (mapped.isUnauthorized) null else mapped.message,
                    canRetry = !mapped.isUnauthorized
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
