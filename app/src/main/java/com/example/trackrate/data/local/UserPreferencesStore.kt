package com.example.trackrate.data.local

import android.content.Context
import com.example.trackrate.domain.model.AccentColor
import com.example.trackrate.domain.model.AppPreferences
import com.example.trackrate.domain.model.AppTextSize
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        migrateLegacyPreferences()
    }

    private val _preferences = MutableStateFlow(read())
    val preferences: StateFlow<AppPreferences> = _preferences.asStateFlow()

    init {
        prefs.registerOnSharedPreferenceChangeListener { _, _ ->
            _preferences.value = read()
        }
    }

    fun read(): AppPreferences = readFrom(prefs)

    fun setDarkMode(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .remove(LEGACY_THEME_MODE)
            .commit()
        _preferences.value = read()
    }

    fun setAccentColor(color: AccentColor) {
        prefs.edit()
            .putString(KEY_ACCENT_COLOR, color.key)
            .commit()
        _preferences.value = read()
    }

    fun setTextSize(size: AppTextSize) {
        prefs.edit()
            .putString(KEY_TEXT_SIZE, size.key)
            .commit()
        _preferences.value = read()
    }

    private fun migrateLegacyPreferences() {
        if (!prefs.contains(KEY_DARK_MODE) && prefs.contains(LEGACY_THEME_MODE)) {
            val legacyDark = prefs.getString(LEGACY_THEME_MODE, null) == "dark"
            prefs.edit()
                .putBoolean(KEY_DARK_MODE, legacyDark)
                .remove(LEGACY_THEME_MODE)
                .commit()
        }
    }

    companion object {
        private const val PREFS_NAME = "trackrate_user_preferences"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_TEXT_SIZE = "text_size"
        private const val LEGACY_THEME_MODE = "theme_mode"

        fun readSync(context: Context): AppPreferences =
            readFrom(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

        private fun readFrom(prefs: android.content.SharedPreferences): AppPreferences =
            AppPreferences(
                darkMode = prefs.getBoolean(KEY_DARK_MODE, false),
                accentColor = AccentColor.fromKey(
                    prefs.getString(KEY_ACCENT_COLOR, AccentColor.PURPLE.key)
                ),
                textSize = AppTextSize.fromKey(
                    prefs.getString(KEY_TEXT_SIZE, AppTextSize.NORMAL.key)
                )
            )
    }
}
