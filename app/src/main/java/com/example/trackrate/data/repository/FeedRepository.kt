package com.example.trackrate.data.repository

import com.example.trackrate.data.remote.TrackRateApi
import com.example.trackrate.domain.model.ActivityFeedItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject constructor(
    private val api: TrackRateApi,
    private val authRepository: AuthRepository
) {

    suspend fun getFeed(): List<ActivityFeedItem> {
        authRepository.currentUserId ?: return emptyList()
        return api.getFeed().map { it.toDomain() }
    }
}
