package com.example.trackrate.domain.model

enum class AccentColor(val key: String) {
    PURPLE("purple"),
    BLUE("blue"),
    RED("red");

    companion object {
        fun fromKey(key: String?): AccentColor =
            entries.firstOrNull { it.key == key } ?: PURPLE
    }
}

enum class AppTextSize(val key: String, val scale: Float) {
    NORMAL("normal", 1f),
    LARGE("large", 1.15f),
    EXTRA_LARGE("extra_large", 1.3f);

    companion object {
        fun fromKey(key: String?): AppTextSize =
            entries.firstOrNull { it.key == key } ?: NORMAL
    }
}

data class AppPreferences(
    val darkMode: Boolean = false,
    val accentColor: AccentColor = AccentColor.PURPLE,
    val textSize: AppTextSize = AppTextSize.NORMAL
)
