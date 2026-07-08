package com.example.trackrate.data.remote.dto

import com.example.trackrate.domain.model.CatalogType
import com.example.trackrate.domain.model.ListItemDetail
import com.example.trackrate.domain.model.MusicList
import com.example.trackrate.domain.model.UserRatingStats
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MusicListDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("is_public") val isPublic: Boolean = false
) {
    fun toDomain() = MusicList(
        id = id,
        title = title,
        description = description,
        isPublic = isPublic
    )
}

@Serializable
data class NewListDto(
    @SerialName("user_id") val userId: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("is_public") val isPublic: Boolean = false
)

@Serializable
data class ListItemDetailDto(
    @SerialName("list_id") val listId: String,
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: String,
    @SerialName("position") val position: Int,
    @SerialName("entity_title") val entityTitle: String? = null,
    @SerialName("entity_subtitle") val entitySubtitle: String? = null
) {
    fun toDomain() = ListItemDetail(
        listId = listId,
        entityType = parseEntityType(entityType),
        entityId = entityId,
        position = position,
        title = entityTitle ?: "—",
        subtitle = entitySubtitle
    )
}

@Serializable
data class NewListItemDto(
    @SerialName("list_id") val listId: String,
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: String,
    @SerialName("position") val position: Int
)

@Serializable
data class UserRatingStatsDto(
    @SerialName("total_ratings") val totalRatings: Int,
    @SerialName("average_rating") val averageRating: Double,
    @SerialName("review_count") val reviewCount: Int
) {
    fun toDomain() = UserRatingStats(
        totalRatings = totalRatings,
        averageRating = averageRating,
        reviewCount = reviewCount
    )
}

fun CatalogType.toEntityTypeString(): String = when (this) {
    CatalogType.ARTIST -> "artist"
    CatalogType.ALBUM -> "album"
    CatalogType.TRACK -> "track"
}
