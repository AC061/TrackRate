package com.example.trackrate.data.repository

import com.example.trackrate.data.local.catalogQueryKey
import com.example.trackrate.data.local.dao.CatalogCacheDao
import com.example.trackrate.data.local.toCachedEntity
import com.example.trackrate.data.local.toCatalogItem
import com.example.trackrate.data.remote.TrackRateApi
import com.example.trackrate.data.remote.dto.ContributorInputDto
import com.example.trackrate.data.remote.dto.NewAlbumDto
import com.example.trackrate.data.remote.dto.NewArtistDto
import com.example.trackrate.data.remote.dto.NewTrackDto
import com.example.trackrate.data.remote.dto.SampleInputDto
import com.example.trackrate.data.remote.dto.toApiType
import com.example.trackrate.data.remote.dto.toDomain
import com.example.trackrate.domain.model.CatalogDetail
import com.example.trackrate.domain.model.CatalogItem
import com.example.trackrate.domain.model.CatalogType
import com.example.trackrate.domain.model.ContributorDraft
import com.example.trackrate.domain.model.SampleDraft
import com.example.trackrate.domain.model.TopRatedTrack
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogRepository @Inject constructor(
    private val api: TrackRateApi,
    private val catalogCacheDao: CatalogCacheDao
) {

    suspend fun search(query: String, type: CatalogType?): List<CatalogItem> {
        val key = catalogQueryKey(query, type)
        return try {
            val results = api.searchCatalog(query.trim(), type?.toApiType())
                .map { it.toDomain() }
                .sortedBy { it.title.lowercase() }
            catalogCacheDao.clearQuery(key)
            catalogCacheDao.insertAll(results.map { it.toCachedEntity(key) })
            results
        } catch (e: Exception) {
            catalogCacheDao.getByQuery(key).map { it.toCatalogItem() }.ifEmpty { throw e }
        }
    }

    suspend fun getApprovedArtists(): List<CatalogItem> =
        api.searchCatalog("", "artist").map { it.toDomain() }

    suspend fun getApprovedTracks(): List<CatalogItem> =
        api.searchCatalog("", "track").map { it.toDomain() }

    suspend fun getApprovedAlbumsByArtist(artistId: String): List<CatalogItem> =
        api.getAlbumsByArtist(artistId).map { it.toDomain() }

    suspend fun getDetail(type: CatalogType, id: String): CatalogDetail? = try {
        api.getCatalogDetail(type.toApiType(), id).toDomain()
    } catch (_: Exception) {
        null
    }

    suspend fun getTopRatedTracks(limit: Int = 20): List<TopRatedTrack> =
        api.getTopRatedTracks(limit).map { it.toDomain() }

    suspend fun submitArtist(name: String, bio: String?): String =
        api.submitArtist(NewArtistDto(name = name.trim(), bio = bio?.trim()?.ifBlank { null })).id

    suspend fun submitAlbum(
        title: String,
        artistId: String,
        releaseYear: Int?,
        description: String?,
        labelId: String?,
        contributors: List<ContributorDraft>
    ): String =
        api.submitAlbum(
            NewAlbumDto(
                title = title.trim(),
                artistId = artistId,
                releaseYear = releaseYear,
                description = description?.trim()?.ifBlank { null },
                labelId = labelId,
                contributors = contributors.map {
                    ContributorInputDto(
                        artistId = it.artistId,
                        role = it.role.apiValue,
                        notes = it.notes?.trim()?.ifBlank { null }
                    )
                }
            )
        ).id

    suspend fun submitTrack(
        title: String,
        artistId: String,
        albumId: String?,
        durationMs: Int?,
        description: String?,
        labelId: String?,
        contributors: List<ContributorDraft>,
        samples: List<SampleDraft>
    ): String =
        api.submitTrack(
            NewTrackDto(
                title = title.trim(),
                artistId = artistId,
                albumId = albumId,
                durationMs = durationMs,
                description = description?.trim()?.ifBlank { null },
                labelId = labelId,
                contributors = contributors.map {
                    ContributorInputDto(
                        artistId = it.artistId,
                        role = it.role.apiValue,
                        notes = it.notes?.trim()?.ifBlank { null }
                    )
                },
                samples = samples.map {
                    SampleInputDto(
                        sampledTrackId = it.sampledTrackId,
                        notes = it.notes?.trim()?.ifBlank { null }
                    )
                }
            )
        ).id
}
