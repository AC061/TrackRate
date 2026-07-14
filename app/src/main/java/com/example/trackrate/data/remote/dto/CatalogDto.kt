package com.example.trackrate.data.remote.dto

import com.example.trackrate.domain.model.CatalogDetail
import com.example.trackrate.domain.model.CatalogItem
import com.example.trackrate.domain.model.CatalogType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Lecturas (vistas approved_*)
// ---------------------------------------------------------------------------
@Serializable
data class ApprovedArtistDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("bio") val bio: String? = null,
    @SerialName("image_url") val imageUrl: String? = null
) {
    fun toItem() = CatalogItem(
        id = id,
        type = CatalogType.ARTIST,
        title = name,
        subtitle = null,
        imageUrl = imageUrl,
        year = null
    )

    fun toDetail() = CatalogDetail(
        id = id,
        type = CatalogType.ARTIST,
        title = name,
        subtitle = null,
        extra = null,
        description = bio,
        imageUrl = imageUrl,
        year = null,
        durationMs = null
    )
}

@Serializable
data class ApprovedAlbumDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("artist_id") val artistId: String,
    @SerialName("artist_name") val artistName: String,
    @SerialName("release_year") val releaseYear: Int? = null,
    @SerialName("cover_url") val coverUrl: String? = null
) {
    fun toItem() = CatalogItem(
        id = id,
        type = CatalogType.ALBUM,
        title = title,
        subtitle = artistName,
        imageUrl = coverUrl,
        year = releaseYear
    )

    fun toDetail() = CatalogDetail(
        id = id,
        type = CatalogType.ALBUM,
        title = title,
        subtitle = artistName,
        extra = null,
        description = null,
        imageUrl = coverUrl,
        year = releaseYear,
        durationMs = null
    )
}

@Serializable
data class ApprovedTrackDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("album_id") val albumId: String? = null,
    @SerialName("album_title") val albumTitle: String? = null,
    @SerialName("artist_id") val artistId: String,
    @SerialName("artist_name") val artistName: String,
    @SerialName("duration_ms") val durationMs: Int? = null,
    @SerialName("cover_url") val coverUrl: String? = null
) {
    fun toItem() = CatalogItem(
        id = id,
        type = CatalogType.TRACK,
        title = title,
        subtitle = artistName,
        imageUrl = coverUrl,
        year = null
    )

    fun toDetail() = CatalogDetail(
        id = id,
        type = CatalogType.TRACK,
        title = title,
        subtitle = artistName,
        extra = albumTitle,
        description = null,
        imageUrl = coverUrl,
        year = null,
        durationMs = durationMs
    )
}

// ---------------------------------------------------------------------------
// Inserts (status/submitted_by los rellena el trigger del backend)
// ---------------------------------------------------------------------------
@Serializable
data class NewArtistDto(
    @SerialName("name") val name: String,
    @SerialName("bio") val bio: String? = null
)

@Serializable
data class ContributorInputDto(
    @SerialName("artist_id") val artistId: String,
    @SerialName("role") val role: String,
    @SerialName("notes") val notes: String? = null
)

@Serializable
data class SampleInputDto(
    @SerialName("sampled_track_id") val sampledTrackId: String,
    @SerialName("notes") val notes: String? = null
)

@Serializable
data class NewAlbumDto(
    @SerialName("title") val title: String,
    @SerialName("artist_id") val artistId: String,
    @SerialName("release_year") val releaseYear: Int? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("label_id") val labelId: String? = null,
    @SerialName("contributors") val contributors: List<ContributorInputDto> = emptyList()
)

@Serializable
data class NewTrackDto(
    @SerialName("title") val title: String,
    @SerialName("artist_id") val artistId: String,
    @SerialName("album_id") val albumId: String? = null,
    @SerialName("duration_ms") val durationMs: Int? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("label_id") val labelId: String? = null,
    @SerialName("contributors") val contributors: List<ContributorInputDto> = emptyList(),
    @SerialName("samples") val samples: List<SampleInputDto> = emptyList()
)
