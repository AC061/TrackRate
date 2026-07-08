package com.example.trackrate.data.remote.dto

import com.example.trackrate.domain.model.CatalogType
import com.example.trackrate.domain.model.DiaryEntry
import com.example.trackrate.domain.model.Rating
import com.example.trackrate.domain.model.RatingStats
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

fun CatalogType.toEntityType(): String = when (this) {
    CatalogType.ARTIST -> "artist"
    CatalogType.ALBUM -> "album"
    CatalogType.TRACK -> "track"
}

fun parseEntityType(raw: String): CatalogType = when (raw) {
    "artist" -> CatalogType.ARTIST
    "album" -> CatalogType.ALBUM
    else -> CatalogType.TRACK
}

@Serializable
data class RatingDto(
    @SerialName("id") val id: String,
    @SerialName("rating") val rating: Double,
    @SerialName("review") val review: String? = null,
    @SerialName("listened_at") val listenedAt: String? = null
) {
    fun toRating() = Rating(id = id, rating = rating, review = review, listenedAt = listenedAt)
}

@Serializable
data class NewRatingDto(
    @SerialName("user_id") val userId: String,
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: String,
    @SerialName("rating") val rating: Double,
    @SerialName("review") val review: String? = null,
    @SerialName("listened_at") val listenedAt: String? = null
)

@Serializable
data class RatingUpdateDto(
    @SerialName("rating") val rating: Double,
    @SerialName("review") val review: String? = null,
    @SerialName("listened_at") val listenedAt: String? = null
)

@Serializable
data class RatingStatsDto(
    @SerialName("average") val average: Double,
    @SerialName("count") val count: Int
) {
    fun toStats() = RatingStats(average = average, count = count)
}

@Serializable
data class RatingDetailDto(
    @SerialName("id") val id: String,
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: String,
    @SerialName("rating") val rating: Double,
    @SerialName("review") val review: String? = null,
    @SerialName("listened_at") val listenedAt: String? = null,
    @SerialName("entity_title") val entityTitle: String? = null,
    @SerialName("entity_subtitle") val entitySubtitle: String? = null
) {
    fun toDiaryEntry() = DiaryEntry(
        id = id,
        entityType = parseEntityType(entityType),
        entityId = entityId,
        title = entityTitle ?: "—",
        subtitle = entitySubtitle,
        rating = rating,
        review = review,
        listenedAt = listenedAt
    )
}
