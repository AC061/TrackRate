package com.example.trackrate.data.remote.dto

import com.example.trackrate.domain.model.ActivityFeedItem
import com.example.trackrate.domain.model.ActivityType
import com.example.trackrate.domain.model.ProfileStats
import com.example.trackrate.util.MediaUrlResolver
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private fun parseActivityType(raw: String): ActivityType = when (raw) {
    "reviewed" -> ActivityType.REVIEWED
    "updated" -> ActivityType.UPDATED
    else -> ActivityType.RATED
}

@Serializable
data class ActivityFeedDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("username") val username: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("activity_type") val activityType: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("rating") val rating: Double,
    @SerialName("review") val review: String? = null,
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: String,
    @SerialName("entity_title") val entityTitle: String? = null,
    @SerialName("entity_subtitle") val entitySubtitle: String? = null
) {
    fun toDomain() = ActivityFeedItem(
        id = id,
        userId = userId,
        username = username,
        displayName = displayName,
        avatarUrl = MediaUrlResolver.resolve(avatarUrl),
        activityType = parseActivityType(activityType),
        createdAt = createdAt,
        rating = rating,
        review = review,
        entityType = parseEntityType(entityType),
        entityId = entityId,
        entityTitle = entityTitle ?: "—",
        entitySubtitle = entitySubtitle
    )
}

@Serializable
data class ProfileStatsDto(
    @SerialName("follower_count") val followerCount: Int,
    @SerialName("following_count") val followingCount: Int,
    @SerialName("rating_count") val ratingCount: Int
) {
    fun toDomain() = ProfileStats(
        followerCount = followerCount,
        followingCount = followingCount,
        ratingCount = ratingCount
    )
}

@Serializable
data class FollowInsertDto(
    @SerialName("follower_id") val followerId: String,
    @SerialName("following_id") val followingId: String
)

@Serializable
data class FollowingIdDto(
    @SerialName("following_id") val followingId: String
)

@Serializable
data class FollowCheckDto(
    @SerialName("follower_id") val followerId: String
)
