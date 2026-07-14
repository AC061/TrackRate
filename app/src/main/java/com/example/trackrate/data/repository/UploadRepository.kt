package com.example.trackrate.data.repository

import android.content.Context
import android.net.Uri
import com.example.trackrate.data.remote.TrackRateApi
import com.example.trackrate.domain.model.CatalogType
import com.example.trackrate.util.ImagePayload
import com.example.trackrate.util.ImageUriReader
import com.example.trackrate.util.MediaUrlResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadRepository @Inject constructor(
    private val api: TrackRateApi,
    @ApplicationContext private val context: Context
) {

    fun readImage(uri: Uri): ImagePayload = ImageUriReader.read(context, uri)

    suspend fun uploadAvatar(payload: ImagePayload): String =
        MediaUrlResolver.resolve(
            api.uploadAvatar(payload.bytes, payload.contentType, payload.fileName).url
        ).orEmpty()

    suspend fun uploadCatalogImage(
        type: CatalogType,
        entityId: String,
        payload: ImagePayload
    ): String = MediaUrlResolver.resolve(
        when (type) {
            CatalogType.ARTIST -> api.uploadArtistImage(
                entityId,
                payload.bytes,
                payload.contentType,
                payload.fileName
            ).url

            CatalogType.ALBUM -> api.uploadAlbumCover(
                entityId,
                payload.bytes,
                payload.contentType,
                payload.fileName
            ).url

            CatalogType.TRACK -> api.uploadTrackCover(
                entityId,
                payload.bytes,
                payload.contentType,
                payload.fileName
            ).url
        }
    ).orEmpty()

    suspend fun uploadListCover(listId: String, payload: ImagePayload): String =
        MediaUrlResolver.resolve(
            api.uploadListCover(listId, payload.bytes, payload.contentType, payload.fileName).url
        ).orEmpty()
}
