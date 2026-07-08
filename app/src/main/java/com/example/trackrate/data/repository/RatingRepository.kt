package com.example.trackrate.data.repository

import com.example.trackrate.data.local.dao.DiaryCacheDao
import com.example.trackrate.data.local.toCachedEntity
import com.example.trackrate.data.local.toDiaryEntry
import com.example.trackrate.data.remote.TrackRateApi
import com.example.trackrate.data.remote.dto.RatingUpsertDto
import com.example.trackrate.data.remote.dto.toApiType
import com.example.trackrate.data.remote.dto.toEntityType
import com.example.trackrate.domain.model.CatalogType
import com.example.trackrate.domain.model.DiaryEntry
import com.example.trackrate.domain.model.Rating
import com.example.trackrate.domain.model.RatingStats
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RatingRepository @Inject constructor(
    private val api: TrackRateApi,
    private val authRepository: AuthRepository,
    private val diaryCacheDao: DiaryCacheDao
) {

    suspend fun getStats(type: CatalogType, id: String): RatingStats? =
        api.getRatingStats(type.toEntityType(), id)?.toStats()

    suspend fun getMyRating(type: CatalogType, id: String): Rating? {
        authRepository.currentUserId ?: return null
        return api.getMyRating(type.toEntityType(), id)?.toRating()
    }

    suspend fun saveRating(
        type: CatalogType,
        id: String,
        rating: Double,
        review: String?,
        listenedAt: String?
    ) {
        authRepository.currentUserId ?: error("Sesión no válida")
        val cleanReview = review?.trim()?.ifBlank { null }
        api.upsertRating(
            RatingUpsertDto(
                entityType = type.toEntityType(),
                entityId = id,
                rating = rating,
                review = cleanReview,
                listenedAt = listenedAt
            )
        )
    }

    suspend fun deleteRating(type: CatalogType, id: String) {
        authRepository.currentUserId ?: return
        api.deleteRating(type.toEntityType(), id)
    }

    suspend fun getMyDiary(): List<DiaryEntry> {
        authRepository.currentUserId ?: return emptyList()
        return try {
            val entries = getUserDiary(authRepository.currentUserId!!)
            diaryCacheDao.clearAll()
            diaryCacheDao.insertAll(entries.map { it.toCachedEntity() })
            entries
        } catch (e: Exception) {
            diaryCacheDao.getAll().map { it.toDiaryEntry() }.ifEmpty { throw e }
        }
    }

    suspend fun getUserDiary(userId: String): List<DiaryEntry> =
        api.getUserDiary(userId).map { it.toDiaryEntry() }
}
