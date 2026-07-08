package com.example.trackrate.data.remote.dto

import com.example.trackrate.domain.model.CatalogSubmission
import com.example.trackrate.domain.model.CatalogType
import com.example.trackrate.domain.model.ModerationStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private fun parseStatus(raw: String): ModerationStatus = when (raw) {
    "approved" -> ModerationStatus.APPROVED
    "rejected" -> ModerationStatus.REJECTED
    else -> ModerationStatus.PENDING
}

@Serializable
data class ArtistRef(
    @SerialName("name") val name: String? = null
)

@Serializable
data class ArtistRowDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("status") val status: String,
    @SerialName("rejection_reason") val rejectionReason: String? = null
) {
    fun toSubmission() = CatalogSubmission(
        id = id,
        type = CatalogType.ARTIST,
        title = name,
        subtitle = null,
        status = parseStatus(status),
        rejectionReason = rejectionReason
    )
}

@Serializable
data class AlbumRowDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("status") val status: String,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("artists") val artist: ArtistRef? = null
) {
    fun toSubmission() = CatalogSubmission(
        id = id,
        type = CatalogType.ALBUM,
        title = title,
        subtitle = artist?.name,
        status = parseStatus(status),
        rejectionReason = rejectionReason
    )
}

@Serializable
data class TrackRowDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("status") val status: String,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("artists") val artist: ArtistRef? = null
) {
    fun toSubmission() = CatalogSubmission(
        id = id,
        type = CatalogType.TRACK,
        title = title,
        subtitle = artist?.name,
        status = parseStatus(status),
        rejectionReason = rejectionReason
    )
}
