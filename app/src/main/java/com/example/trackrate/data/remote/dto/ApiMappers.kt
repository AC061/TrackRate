package com.example.trackrate.data.remote.dto

import com.example.trackrate.domain.model.CatalogContributor
import com.example.trackrate.domain.model.CatalogDetail
import com.example.trackrate.domain.model.CatalogItem
import com.example.trackrate.domain.model.CatalogSample
import com.example.trackrate.domain.model.CatalogSubmission
import com.example.trackrate.domain.model.CatalogType
import com.example.trackrate.domain.model.ModerationStatus
import com.example.trackrate.util.MediaUrlResolver

fun parseCatalogType(raw: String): CatalogType = when (raw) {
    "artist" -> CatalogType.ARTIST
    "album" -> CatalogType.ALBUM
    else -> CatalogType.TRACK
}

fun CatalogType.toApiType(): String = when (this) {
    CatalogType.ARTIST -> "artist"
    CatalogType.ALBUM -> "album"
    CatalogType.TRACK -> "track"
}

fun CatalogItemDto.toDomain(): CatalogItem = CatalogItem(
    id = id,
    type = parseCatalogType(type),
    title = title,
    subtitle = subtitle,
    imageUrl = MediaUrlResolver.resolve(imageUrl),
    year = year
)

fun CatalogDetailDto.toDomain(): CatalogDetail = CatalogDetail(
    id = id,
    type = parseCatalogType(type),
    title = title,
    subtitle = subtitle,
    extra = extra,
    description = description,
    imageUrl = MediaUrlResolver.resolve(imageUrl),
    year = year,
    durationMs = durationMs,
    label = label,
    contributors = contributors.map {
        CatalogContributor(
            artistId = it.artistId,
            artistName = it.artistName,
            role = it.role,
            roleLabel = it.roleLabel,
            notes = it.notes
        )
    },
    samples = samples.map {
        CatalogSample(
            trackId = it.trackId,
            title = it.title,
            artistName = it.artistName,
            albumTitle = it.albumTitle,
            notes = it.notes
        )
    }
)

private fun parseModerationStatus(raw: String): ModerationStatus = when (raw) {
    "approved" -> ModerationStatus.APPROVED
    "rejected" -> ModerationStatus.REJECTED
    else -> ModerationStatus.PENDING
}

fun SubmissionDto.toSubmission(): CatalogSubmission = CatalogSubmission(
    id = id,
    type = parseCatalogType(type),
    title = title,
    subtitle = subtitle,
    status = parseModerationStatus(status),
    rejectionReason = rejectionReason
)
