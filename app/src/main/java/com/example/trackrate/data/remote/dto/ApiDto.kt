package com.example.trackrate.data.remote.dto

import com.example.trackrate.domain.model.UserProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecordLabelDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String
)

@Serializable
data class CreateRecordLabelDto(
    @SerialName("name") val name: String
)

@Serializable
data class TopRatedTrackDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("subtitle") val subtitle: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("average_rating") val averageRating: Double,
    @SerialName("rating_count") val ratingCount: Int
)

@Serializable
data class UploadResponseDto(
    @SerialName("url") val url: String
)

@Serializable
data class LoginRequestDto(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String
)

@Serializable
data class RegisterRequestDto(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String
)

@Serializable
data class ChangePasswordRequestDto(
    @SerialName("current_password")
    val currentPassword: String,

    @SerialName("new_password")
    val newPassword: String,

    @SerialName("confirm_password")
    val confirmPassword: String
)

@Serializable
data class MessageResponseDto(
    val detail: String
)
@Serializable
data class TokenResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("user") val user: UserResponseDto
)

@Serializable
data class UserResponseDto(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String,
    @SerialName("profile") val profile: ProfileDto
)

@Serializable
data class ApiErrorDto(
    @SerialName("detail") val detail: String? = null
)

@Serializable
data class CatalogItemDto(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("title") val title: String,
    @SerialName("subtitle") val subtitle: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("year") val year: Int? = null
)

@Serializable
data class ContributorResponseDto(
    @SerialName("artist_id") val artistId: String,
    @SerialName("artist_name") val artistName: String,
    @SerialName("role") val role: String,
    @SerialName("role_label") val roleLabel: String,
    @SerialName("notes") val notes: String? = null
)

@Serializable
data class SampleResponseDto(
    @SerialName("track_id") val trackId: String,
    @SerialName("title") val title: String,
    @SerialName("artist_name") val artistName: String,
    @SerialName("album_title") val albumTitle: String? = null,
    @SerialName("notes") val notes: String? = null
)

@Serializable
data class CatalogDetailDto(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("title") val title: String,
    @SerialName("subtitle") val subtitle: String? = null,
    @SerialName("extra") val extra: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("year") val year: Int? = null,
    @SerialName("duration_ms") val durationMs: Int? = null,
    @SerialName("artist_id") val artistId: String? = null,
    @SerialName("album_id") val albumId: String? = null,
    @SerialName("label") val label: String? = null,
    @SerialName("label_id") val labelId: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("contributors") val contributors: List<ContributorResponseDto> = emptyList(),
    @SerialName("samples") val samples: List<SampleResponseDto> = emptyList()
)

@Serializable
data class SubmissionDto(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("title") val title: String,
    @SerialName("subtitle") val subtitle: String? = null,
    @SerialName("status") val status: String,
    @SerialName("rejection_reason") val rejectionReason: String? = null
)

@Serializable
data class ModerationActionDto(
    @SerialName("action") val action: String,
    @SerialName("rejection_reason") val rejectionReason: String? = null
)

@Serializable
data class SetAdminRequestDto(
    @SerialName("make_admin") val makeAdmin: Boolean
)

@Serializable
data class RatingUpsertDto(
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: String,
    @SerialName("rating") val rating: Double,
    @SerialName("review") val review: String? = null,
    @SerialName("listened_at") val listenedAt: String? = null
)

@Serializable
data class CreateListRequestDto(
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("is_public") val isPublic: Boolean = false
)

@Serializable
data class AddListItemRequestDto(
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: String
)

fun UserResponseDto.toProfile(): UserProfile = profile.toDomain()
