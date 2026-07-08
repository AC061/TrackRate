package com.example.trackrate.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackrate.data.repository.RatingRepository
import com.example.trackrate.domain.model.DiaryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiaryUiState(
    val isLoading: Boolean = true,
    val entries: List<DiaryEntry> = emptyList(),
    val message: String? = null
)

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val ratingRepository: RatingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true, message = null)
        viewModelScope.launch {
            try {
                val entries = ratingRepository.getMyDiary()
                _uiState.value = DiaryUiState(isLoading = false, entries = entries)
            } catch (e: Exception) {
                _uiState.value = DiaryUiState(
                    isLoading = false,
                    message = e.message ?: "Error al cargar el diario"
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
