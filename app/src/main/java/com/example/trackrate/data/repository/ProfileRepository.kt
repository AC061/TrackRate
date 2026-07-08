package com.example.trackrate.data.repository

import com.example.trackrate.data.remote.TrackRateApi
import com.example.trackrate.data.remote.dto.ProfileUpdateDto
import com.example.trackrate.data.remote.dto.toProfile
import com.example.trackrate.domain.model.ProfileStats
import com.example.trackrate.domain.model.UserProfile
import com.example.trackrate.domain.model.UserRatingStats
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val api: TrackRateApi,
    private val authRepository: AuthRepository
) {

    suspend fun getCurrentProfile(): UserProfile? {
        authRepository.currentUserId ?: return null
        return try {
            api.me().toProfile()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getProfile(userId: String): UserProfile? {
        val current = getCurrentProfile() ?: return null
        return if (current.id == userId) current else null
    }

    suspend fun getProfileByUsername(username: String): UserProfile? = try {
        api.getProfileByUsername(username).toDomain()
    } catch (_: Exception) {
        null
    }

    suspend fun getProfileStats(userId: String): ProfileStats? = try {
        api.getProfileStats(userId).toDomain()
    } catch (_: Exception) {
        null
    }

    suspend fun getUserRatingStats(userId: String): UserRatingStats? = try {
        api.getUserRatingStats(userId)?.toDomain()
    } catch (_: Exception) {
        null
    }

    suspend fun updateProfile(
        username: String,
        displayName: String?,
        bio: String?
    ): UserProfile {
        return api.updateProfile(
            ProfileUpdateDto(
                username = username.trim(),
                displayName = displayName?.trim()?.ifBlank { null },
                bio = bio?.trim()?.ifBlank { null }
            )
        ).toDomain()
    }
}
