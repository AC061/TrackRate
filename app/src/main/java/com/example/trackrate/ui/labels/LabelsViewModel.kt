package com.example.trackrate.ui.labels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackrate.data.repository.LabelRepository
import com.example.trackrate.domain.model.RecordLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LabelsUiState(
    val labels: List<RecordLabel> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null
)

@HiltViewModel
class LabelsViewModel @Inject constructor(
    private val labelRepository: LabelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LabelsUiState())
    val uiState: StateFlow<LabelsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true, message = null)
        viewModelScope.launch {
            try {
                val labels = labelRepository.getLabels()
                _uiState.value = LabelsUiState(isLoading = false, labels = labels)
            } catch (e: Exception) {
                _uiState.value = LabelsUiState(
                    isLoading = false,
                    message = e.message ?: "Error al cargar sellos"
                )
            }
        }
    }

    fun createLabel(name: String) {
        viewModelScope.launch {
            try {
                labelRepository.createLabel(name)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = e.message ?: "No se pudo crear el sello"
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
