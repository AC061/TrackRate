package com.example.trackrate.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import com.example.trackrate.R
import com.example.trackrate.data.local.UserPreferencesStore
import com.example.trackrate.domain.model.AccentColor
import com.example.trackrate.domain.model.AppPreferences
import com.example.trackrate.domain.model.AppTextSize
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {

    fun apply(preferences: AppPreferences) {
        val targetMode = if (preferences.darkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode)
        }
    }

    fun themeStyle(accent: AccentColor): Int = when (accent) {
        AccentColor.PURPLE -> R.style.Theme_TrackRate_Purple_NoActionBar
        AccentColor.BLUE -> R.style.Theme_TrackRate_Blue_NoActionBar
        AccentColor.RED -> R.style.Theme_TrackRate_Red_NoActionBar
    }

    fun wrapContext(base: Context): Context {
        val preferences = UserPreferencesStore.readSync(base)
        val config = Configuration(base.resources.configuration)
        config.fontScale = preferences.textSize.scale
        return base.createConfigurationContext(config)
    }

    fun registerActivityThemes(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
                val accent = UserPreferencesStore.readSync(activity).accentColor
                activity.setTheme(themeStyle(accent))
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
