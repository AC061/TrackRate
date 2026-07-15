package com.example.trackrate.data.repository

import com.example.trackrate.data.local.UserPreferencesStore
import com.example.trackrate.domain.model.AccentColor
import com.example.trackrate.domain.model.AppPreferences
import com.example.trackrate.domain.model.AppTextSize
import com.example.trackrate.util.ThemeManager
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val store: UserPreferencesStore
) {
    val preferences: StateFlow<AppPreferences> = store.preferences

    fun current(): AppPreferences = store.read()

    fun applyStoredTheme() {
        ThemeManager.apply(store.read())
    }

    fun setDarkMode(enabled: Boolean) {
        store.setDarkMode(enabled)
        ThemeManager.apply(store.read())
    }

    fun setAccentColor(color: AccentColor) {
        store.setAccentColor(color)
    }

    fun setTextSize(size: AppTextSize) {
        store.setTextSize(size)
    }
}
