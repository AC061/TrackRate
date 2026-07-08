package com.example.trackrate.data.repository

import com.example.trackrate.data.remote.TrackRateApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FollowRepository @Inject constructor(
    private val api: TrackRateApi,
    private val authRepository: AuthRepository
) {

    suspend fun getFollowingIds(): List<String> {
        authRepository.currentUserId ?: return emptyList()
        return api.getFollowing().map { it.followingId }
    }

    suspend fun follow(userId: String) {
        requireNotNull(authRepository.currentUserId) { "Sesión no válida" }
        check(authRepository.currentUserId != userId) { "No puedes seguirte a ti mismo" }
        api.follow(userId)
    }

    suspend fun unfollow(userId: String) {
        authRepository.currentUserId ?: return
        api.unfollow(userId)
    }

    suspend fun isFollowing(userId: String): Boolean {
        authRepository.currentUserId ?: return false
        return api.isFollowing(userId).isNotEmpty()
    }
}
