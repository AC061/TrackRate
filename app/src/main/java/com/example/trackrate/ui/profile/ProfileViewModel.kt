package com.example.trackrate.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackrate.data.repository.AuthRepository
import com.example.trackrate.data.repository.FollowRepository
import com.example.trackrate.data.repository.ProfileRepository
import com.example.trackrate.data.repository.RatingRepository
import com.example.trackrate.domain.model.DiaryEntry
import com.example.trackrate.domain.model.ProfileStats
import com.example.trackrate.domain.model.UserProfile
import com.example.trackrate.domain.model.UserRatingStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val stats: ProfileStats? = null,
    val ratingStats: UserRatingStats? = null,
    val ratings: List<DiaryEntry> = emptyList(),
    val isFollowing: Boolean = false,
    val isOwnProfile: Boolean = false,
    val isWorking: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val followRepository: FollowRepository,
    private val ratingRepository: RatingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var loadedUsername: String? = null

    fun load(username: String) {
        loadedUsername = username
        _uiState.value = ProfileUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val profile = profileRepository.getProfileByUsername(username)
                if (profile == null) {
                    _uiState.value = ProfileUiState(
                        isLoading = false,
                        message = "Usuario no encontrado"
                    )
                    return@launch
                }
                val currentUserId = authRepository.currentUserId
                val isOwn = currentUserId == profile.id
                val stats = profileRepository.getProfileStats(profile.id)
                val ratingStats = profileRepository.getUserRatingStats(profile.id)
                val ratings = ratingRepository.getUserDiary(profile.id)
                val isFollowing = if (!isOwn && currentUserId != null) {
                    followRepository.isFollowing(profile.id)
                } else {
                    false
                }
                _uiState.value = ProfileUiState(
                    isLoading = false,
                    profile = profile,
                    stats = stats,
                    ratingStats = ratingStats,
                    ratings = ratings,
                    isFollowing = isFollowing,
                    isOwnProfile = isOwn
                )
            } catch (e: Exception) {
                _uiState.value = ProfileUiState(
                    isLoading = false,
                    message = e.message ?: "Error al cargar el perfil"
                )
            }
        }
    }

    fun toggleFollow() {
        val profile = _uiState.value.profile ?: return
        if (_uiState.value.isOwnProfile) return

        val wasFollowing = _uiState.value.isFollowing
        _uiState.value = _uiState.value.copy(isWorking = true, message = null)
        viewModelScope.launch {
            try {
                if (wasFollowing) {
                    followRepository.unfollow(profile.id)
                } else {
                    followRepository.follow(profile.id)
                }
                val stats = profileRepository.getProfileStats(profile.id)
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    isFollowing = !wasFollowing,
                    stats = stats,
                    message = if (wasFollowing) "Dejaste de seguir a @${profile.username}"
                    else "Ahora sigues a @${profile.username}"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    message = e.message ?: "No se pudo actualizar el seguimiento"
                )
            }
        }
    }

    fun reload() {
        loadedUsername?.let { load(it) }
    }

    fun consumeMessage() {
        if (_uiState.value.message != null) {
            _uiState.value = _uiState.value.copy(message = null)
        }
    }
}
