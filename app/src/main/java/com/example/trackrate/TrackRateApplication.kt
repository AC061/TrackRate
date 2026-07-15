package com.example.trackrate

import android.app.Application
import com.example.trackrate.di.PreferencesEntryPoint
import com.example.trackrate.util.ThemeManager
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.EntryPointAccessors

@HiltAndroidApp
class TrackRateApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val preferencesRepository = EntryPointAccessors.fromApplication(
            this,
            PreferencesEntryPoint::class.java
        ).preferencesRepository()
        preferencesRepository.applyStoredTheme()
        ThemeManager.registerActivityThemes(this)
    }
}
