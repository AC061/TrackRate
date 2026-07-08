package com.example.trackrate.domain.model

enum class ActivityType { RATED, REVIEWED, UPDATED }

/** Entrada del feed de inicio (actividad de usuarios seguidos + propia). */
data class ActivityFeedItem(
    val id: String,
    val userId: String,
    val username: String,
    val displayName: String?,
    val avatarUrl: String?,
    val activityType: ActivityType,
    val createdAt: String,
    val rating: Double,
    val review: String?,
    val entityType: CatalogType,
    val entityId: String,
    val entityTitle: String,
    val entitySubtitle: String?
)

/** Estadísticas sociales de un perfil público. */
data class ProfileStats(
    val followerCount: Int,
    val followingCount: Int,
    val ratingCount: Int
)

/** Estadísticas de valoración de un usuario. */
data class UserRatingStats(
    val totalRatings: Int,
    val averageRating: Double,
    val reviewCount: Int
)
