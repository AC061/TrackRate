package com.example.trackrate.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackrate.data.repository.PreferencesRepository
import com.example.trackrate.domain.model.AccentColor
import com.example.trackrate.domain.model.AppPreferences
import com.example.trackrate.domain.model.AppTextSize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = preferencesRepository.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = preferencesRepository.current()
        )

    fun current(): AppPreferences = preferencesRepository.current()

    fun setDarkMode(enabled: Boolean) {
        preferencesRepository.setDarkMode(enabled)
    }

    fun setAccentColor(color: AccentColor) {
        preferencesRepository.setAccentColor(color)
    }

    fun setTextSize(size: AppTextSize) {
        preferencesRepository.setTextSize(size)
    }
}
