package com.example.trackrate.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackrate.data.repository.CatalogRepository
import com.example.trackrate.data.repository.ListRepository
import com.example.trackrate.data.repository.RatingRepository
import com.example.trackrate.domain.model.CatalogDetail
import com.example.trackrate.domain.model.CatalogType
import com.example.trackrate.domain.model.MusicList
import com.example.trackrate.domain.model.Rating
import com.example.trackrate.domain.model.RatingStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val detail: CatalogDetail? = null,
    val stats: RatingStats? = null,
    val myRating: Rating? = null,
    val message: String? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val ratingRepository: RatingRepository,
    private val listRepository: ListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var currentType: CatalogType? = null
    private var currentId: String? = null

    fun load(type: CatalogType, id: String) {
        currentType = type
        currentId = id
        _uiState.value = DetailUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val detail = catalogRepository.getDetail(type, id)
                val stats = ratingRepository.getStats(type, id)
                val myRating = ratingRepository.getMyRating(type, id)
                _uiState.value = DetailUiState(
                    isLoading = false,
                    detail = detail,
                    stats = stats,
                    myRating = myRating,
                    message = if (detail == null) "No se encontró el contenido" else null
                )
            } catch (e: Exception) {
                _uiState.value = DetailUiState(
                    isLoading = false,
                    message = e.message ?: "Error al cargar el detalle"
                )
            }
        }
    }

    fun saveRating(rating: Double, review: String?, listenedAt: String?) {
        val type = currentType ?: return
        val id = currentId ?: return
        viewModelScope.launch {
            try {
                ratingRepository.saveRating(type, id, rating, review, listenedAt)
                refreshRatings(type, id, "Valoración guardada")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = e.message ?: "No se pudo guardar")
            }
        }
    }

    fun deleteRating() {
        val type = currentType ?: return
        val id = currentId ?: return
        viewModelScope.launch {
            try {
                ratingRepository.deleteRating(type, id)
                refreshRatings(type, id, "Valoración eliminada")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = e.message ?: "No se pudo eliminar")
            }
        }
    }

    private suspend fun refreshRatings(type: CatalogType, id: String, message: String) {
        val stats = ratingRepository.getStats(type, id)
        val myRating = ratingRepository.getMyRating(type, id)
        _uiState.value = _uiState.value.copy(
            stats = stats,
            myRating = myRating,
            message = message
        )
    }

    fun consumeMessage() {
        if (_uiState.value.message != null) {
            _uiState.value = _uiState.value.copy(message = null)
        }
    }

    suspend fun getMyLists(): List<MusicList> = listRepository.getMyLists()

    fun addToList(listId: String) {
        val type = currentType ?: return
        val id = currentId ?: return
        viewModelScope.launch {
            try {
                listRepository.addItem(listId, type, id)
                _uiState.value = _uiState.value.copy(message = "Añadido a la lista")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = e.message ?: "No se pudo añadir")
            }
        }
    }
}
