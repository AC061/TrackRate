package com.example.trackrate.data.repository

import com.example.trackrate.data.remote.TrackRateApi
import com.example.trackrate.data.remote.dto.ModerationActionDto
import com.example.trackrate.data.remote.dto.toApiType
import com.example.trackrate.data.remote.dto.toSubmission
import com.example.trackrate.data.remote.dto.toDomain
import com.example.trackrate.domain.model.CatalogDetail
import com.example.trackrate.domain.model.CatalogSubmission
import com.example.trackrate.domain.model.CatalogType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModerationRepository @Inject constructor(
    private val api: TrackRateApi
) {

    suspend fun getPending(): List<CatalogSubmission> =
        api.getPendingSubmissions().map { it.toSubmission() }

    suspend fun getMySubmissions(): List<CatalogSubmission> =
        api.getMySubmissions().map { it.toSubmission() }

    suspend fun getSubmissionDetail(type: CatalogType, id: String): CatalogDetail =
        api.getModerationDetail(type.toApiType(), id).toDomain()

    suspend fun approve(type: CatalogType, id: String) {
        api.moderate(type.toApiType(), id, ModerationActionDto(action = "approve"))
    }

    suspend fun reject(type: CatalogType, id: String, reason: String) {
        api.moderate(
            type.toApiType(),
            id,
            ModerationActionDto(action = "reject", rejectionReason = reason.trim())
        )
    }

    suspend fun setUserAdmin(username: String, makeAdmin: Boolean) {
        api.setUserAdmin(username, makeAdmin)
    }
}
