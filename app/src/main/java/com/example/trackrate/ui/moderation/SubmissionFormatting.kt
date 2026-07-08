package com.example.trackrate.ui.moderation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.trackrate.R
import com.example.trackrate.domain.model.CatalogType
import com.example.trackrate.domain.model.ModerationStatus

@StringRes
fun CatalogType.labelRes(): Int = when (this) {
    CatalogType.ARTIST -> R.string.catalog_type_artist
    CatalogType.ALBUM -> R.string.catalog_type_album
    CatalogType.TRACK -> R.string.catalog_type_track
}

@DrawableRes
fun CatalogType.iconRes(): Int = when (this) {
    CatalogType.ARTIST -> R.drawable.ic_mdi_account_music
    CatalogType.ALBUM -> R.drawable.ic_mdi_album
    CatalogType.TRACK -> R.drawable.ic_mdi_music_note
}

@StringRes
fun ModerationStatus.labelRes(): Int = when (this) {
    ModerationStatus.PENDING -> R.string.mod_status_pending
    ModerationStatus.APPROVED -> R.string.mod_status_approved
    ModerationStatus.REJECTED -> R.string.mod_status_rejected
}

@DrawableRes
fun ModerationStatus.iconRes(): Int = when (this) {
    ModerationStatus.PENDING -> R.drawable.ic_mdi_clock_outline
    ModerationStatus.APPROVED -> R.drawable.ic_mdi_check_circle
    ModerationStatus.REJECTED -> R.drawable.ic_mdi_close_circle
}
