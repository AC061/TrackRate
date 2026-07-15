package com.example.trackrate.domain.model

data class UserProfile(
    val id: String,
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val displayName: String?,
    val bio: String?,
    val avatarUrl: String?,
    val isAdmin: Boolean
) {
    fun fullName(): String? {
        val parts = listOfNotNull(
            firstName?.trim()?.takeIf { it.isNotEmpty() },
            lastName?.trim()?.takeIf { it.isNotEmpty() }
        )
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }
}
