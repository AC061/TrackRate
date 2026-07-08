package com.example.trackrate.domain.model

data class UserProfile(
    val id: String,
    val username: String,
    val displayName: String?,
    val bio: String?,
    val avatarUrl: String?,
    val isAdmin: Boolean
)
