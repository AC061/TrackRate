package com.example.trackrate.data.repository

import com.example.trackrate.data.local.catalogQueryKey
import com.example.trackrate.data.local.dao.CatalogCacheDao
import com.example.trackrate.data.local.toCachedEntity
import com.example.trackrate.data.local.toCatalogItem
import com.example.trackrate.data.remote.TrackRateApi
import com.example.trackrate.data.remote.dto.NewAlbumDto
import com.example.trackrate.data.remote.dto.NewArtistDto
import com.example.trackrate.data.remote.dto.NewTrackDto
import com.example.trackrate.data.remote.dto.toApiType
import com.example.trackrate.data.remote.dto.toDomain
import com.example.trackrate.domain.model.CatalogDetail
import com.example.trackrate.domain.model.CatalogItem
import com.example.trackrate.domain.model.CatalogType
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

    suspend fun getApprovedAlbumsByArtist(artistId: String): List<CatalogItem> =
        api.getAlbumsByArtist(artistId).map { it.toDomain() }

    suspend fun getDetail(type: CatalogType, id: String): CatalogDetail? = try {
        api.getCatalogDetail(type.toApiType(), id).toDomain()
    } catch (_: Exception) {
        null
    }

    suspend fun submitArtist(name: String, bio: String?): String =
        api.submitArtist(NewArtistDto(name = name.trim(), bio = bio?.trim()?.ifBlank { null })).id

    suspend fun submitAlbum(title: String, artistId: String, releaseYear: Int?): String =
        api.submitAlbum(
            NewAlbumDto(
                title = title.trim(),
                artistId = artistId,
                releaseYear = releaseYear
            )
        ).id

    suspend fun submitTrack(
        title: String,
        artistId: String,
        albumId: String?,
        durationMs: Int?
    ): String =
        api.submitTrack(
            NewTrackDto(
                title = title.trim(),
                artistId = artistId,
                albumId = albumId,
                durationMs = durationMs
            )
        ).id
}
