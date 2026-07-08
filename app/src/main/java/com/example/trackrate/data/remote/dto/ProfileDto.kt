package com.example.trackrate.data.remote.dto

import com.example.trackrate.domain.model.UserProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false
) {
    fun toDomain(): UserProfile = UserProfile(
        id = id,
        username = username,
        displayName = displayName,
        bio = bio,
        avatarUrl = avatarUrl,
        isAdmin = isAdmin
    )
}

@Serializable
data class ProfileUpdateDto(
    @SerialName("username") val username: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("bio") val bio: String? = null
)
