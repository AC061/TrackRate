package com.example.trackrate.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackrate.data.repository.ProfileRepository
import com.example.trackrate.data.repository.UploadRepository
import com.example.trackrate.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val uploadRepository: UploadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val profile = profileRepository.getCurrentProfile()
                _uiState.value = _uiState.value.copy(isLoading = false, profile = profile)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = e.message ?: "No se pudo cargar el perfil"
                )
            }
        }
    }

    fun uploadAvatar(uri: Uri) {
        _uiState.value = _uiState.value.copy(isUploadingAvatar = true, message = null)
        viewModelScope.launch {
            try {
                val payload = uploadRepository.readImage(uri)
                uploadRepository.uploadAvatar(payload)
                val profile = profileRepository.getCurrentProfile()
                _uiState.value = _uiState.value.copy(
                    isUploadingAvatar = false,
                    profile = profile,
                    message = "Foto de perfil actualizada"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploadingAvatar = false,
                    message = e.message ?: "No se pudo subir la imagen"
                )
            }
        }
    }

    fun saveProfile(
        username: String,
        firstName: String,
        lastName: String,
        displayName: String,
        bio: String
    ) {
        val validationError = validate(username, firstName, lastName)
        if (validationError != null) {
            _uiState.value = _uiState.value.copy(message = validationError)
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true, message = null)
        viewModelScope.launch {
            try {
                val updated = profileRepository.updateProfile(
                    username = username,
                    firstName = firstName,
                    lastName = lastName,
                    displayName = displayName,
                    bio = bio
                )
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    profile = updated,
                    message = "Perfil actualizado"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    message = e.message ?: "No se pudo guardar el perfil"
                )
            }
        }
    }

    fun consumeMessage() {
        if (_uiState.value.message != null) {
            _uiState.value = _uiState.value.copy(message = null)
        }
    }

    private fun validate(username: String, firstName: String, lastName: String): String? {
        val trimmed = username.trim()
        if (!Regex("^[a-z0-9_]{3,30}$").matches(trimmed)) {
            return "El usuario debe tener 3-30 caracteres (minúsculas, números o _)"
        }
        if (firstName.trim().isEmpty()) {
            return "El nombre es obligatorio"
        }
        if (lastName.trim().isEmpty()) {
            return "El apellido es obligatorio"
        }
        return null
    }
}
