package com.example.trackrate.data.remote.dto

import com.example.trackrate.domain.model.UserProfile
import com.example.trackrate.util.MediaUrlResolver
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false
) {
    fun toDomain(): UserProfile = UserProfile(
        id = id,
        username = username,
        firstName = firstName,
        lastName = lastName,
        displayName = displayName,
        bio = bio,
        avatarUrl = MediaUrlResolver.resolve(avatarUrl),
        isAdmin = isAdmin
    )
}

@Serializable
data class ProfileUpdateDto(
    @SerialName("username") val username: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("bio") val bio: String? = null
)

@Serializable
data class ChangePasswordRequestDto(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
    @SerialName("confirm_password") val confirmPassword: String
)

@Serializable
data class MessageResponseDto(
    @SerialName("detail") val detail: String
)
