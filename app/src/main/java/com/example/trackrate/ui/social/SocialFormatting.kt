package com.example.trackrate.ui.social

import android.content.Context
import android.text.format.DateUtils
import com.example.trackrate.R
import com.example.trackrate.domain.model.ActivityFeedItem
import com.example.trackrate.domain.model.ActivityType
import java.time.Instant

fun ActivityFeedItem.displayNameOrUsername(): String =
    displayName?.takeIf { it.isNotBlank() } ?: username

fun ActivityFeedItem.activityText(context: Context): String {
    val name = displayNameOrUsername()
    return when (activityType) {
        ActivityType.RATED -> context.getString(R.string.feed_activity_rated, name, entityTitle)
        ActivityType.REVIEWED -> context.getString(R.string.feed_activity_reviewed, name, entityTitle)
        ActivityType.UPDATED -> context.getString(R.string.feed_activity_updated, name, entityTitle)
    }
}

fun formatRelativeTime(isoTimestamp: String): String {
    return try {
        val millis = Instant.parse(isoTimestamp).toEpochMilli()
        DateUtils.getRelativeTimeSpanString(
            millis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    } catch (_: Exception) {
        isoTimestamp.take(10)
    }
}
