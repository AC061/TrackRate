package com.example.trackrate.ui

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.trackrate.util.ThemeManager

open class ThemedAppCompatActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemeManager.wrapContext(newBase))
    }
}
