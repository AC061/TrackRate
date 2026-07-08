package com.example.trackrate.data.remote.dto

import com.example.trackrate.domain.model.CatalogDetail
import com.example.trackrate.domain.model.CatalogItem
import com.example.trackrate.domain.model.CatalogSubmission
import com.example.trackrate.domain.model.CatalogType
import com.example.trackrate.domain.model.ModerationStatus

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
    imageUrl = imageUrl,
    year = year
)

fun CatalogDetailDto.toDomain(): CatalogDetail = CatalogDetail(
    id = id,
    type = parseCatalogType(type),
    title = title,
    subtitle = subtitle,
    extra = extra,
    description = description,
    imageUrl = imageUrl,
    year = year,
    durationMs = durationMs
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
