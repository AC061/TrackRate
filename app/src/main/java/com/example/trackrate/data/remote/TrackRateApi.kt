package com.example.trackrate.data.remote

import com.example.trackrate.BuildConfig
import com.example.trackrate.data.local.TokenStore
import com.example.trackrate.data.remote.dto.AddListItemRequestDto
import com.example.trackrate.data.remote.dto.ApiErrorDto
import com.example.trackrate.data.remote.dto.CatalogDetailDto
import com.example.trackrate.data.remote.dto.CatalogItemDto
import com.example.trackrate.data.remote.dto.CreateListRequestDto
import com.example.trackrate.data.remote.dto.LoginRequestDto
import com.example.trackrate.data.remote.dto.ModerationActionDto
import com.example.trackrate.data.remote.dto.ProfileDto
import com.example.trackrate.data.remote.dto.ProfileUpdateDto
import com.example.trackrate.data.remote.dto.RatingDetailDto
import com.example.trackrate.data.remote.dto.RatingDto
import com.example.trackrate.data.remote.dto.RatingStatsDto
import com.example.trackrate.data.remote.dto.RatingUpsertDto
import com.example.trackrate.data.remote.dto.RegisterRequestDto
import com.example.trackrate.data.remote.dto.SetAdminRequestDto
import com.example.trackrate.data.remote.dto.SubmissionDto
import com.example.trackrate.data.remote.dto.TokenResponseDto
import com.example.trackrate.data.remote.dto.ActivityFeedDto
import com.example.trackrate.data.remote.dto.FollowCheckDto
import com.example.trackrate.data.remote.dto.FollowingIdDto
import com.example.trackrate.data.remote.dto.ListItemDetailDto
import com.example.trackrate.data.remote.dto.MusicListDto
import com.example.trackrate.data.remote.dto.NewAlbumDto
import com.example.trackrate.data.remote.dto.NewArtistDto
import com.example.trackrate.data.remote.dto.NewTrackDto
import com.example.trackrate.data.remote.dto.ProfileStatsDto
import com.example.trackrate.data.remote.dto.UserRatingStatsDto
import com.example.trackrate.data.remote.dto.UserResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import com.example.trackrate.data.remote.dto.UploadResponseDto
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRateApi @Inject constructor(
    private val client: HttpClient,
    private val tokenStore: TokenStore
) {
    suspend fun login(email: String, password: String): TokenResponseDto =
        postPublic("/auth/login", LoginRequestDto(email, password))

    suspend fun register(email: String, password: String): TokenResponseDto =
        postPublic("/auth/register", RegisterRequestDto(email, password))

    suspend fun me(): UserResponseDto = get("/auth/me")

    suspend fun getProfileByUsername(username: String): ProfileDto =
        get("/profiles/${username.trim()}")

    suspend fun updateProfile(body: ProfileUpdateDto): ProfileDto =
        patch("/me/profile", body)

    suspend fun getProfileStats(userId: String): ProfileStatsDto =
        get("/users/$userId/stats")

    suspend fun getUserRatingStats(userId: String): UserRatingStatsDto? =
        get("/users/$userId/rating-stats")

    suspend fun searchCatalog(query: String, type: String?): List<CatalogItemDto> =
        get("/catalog/search") {
            url.parameters.append("q", query)
            if (type != null) url.parameters.append("type", type)
        }

    suspend fun getCatalogDetail(type: String, id: String): CatalogDetailDto =
        get("/catalog/$type/$id")

    suspend fun getAlbumsByArtist(artistId: String): List<CatalogItemDto> =
        get("/catalog/artists/$artistId/albums")

    suspend fun getRatingStats(type: String, id: String): RatingStatsDto? =
        get("/catalog/$type/$id/rating-stats")

    suspend fun submitArtist(body: NewArtistDto): CatalogDetailDto =
        post("/catalog/artists", body)

    suspend fun submitAlbum(body: NewAlbumDto): CatalogDetailDto =
        post("/catalog/albums", body)

    suspend fun submitTrack(body: NewTrackDto): CatalogDetailDto =
        post("/catalog/tracks", body)

    suspend fun getMyRating(entityType: String, entityId: String): RatingDto? =
        get("/me/ratings") {
            url.parameters.append("entity_type", entityType)
            url.parameters.append("entity_id", entityId)
        }

    suspend fun upsertRating(body: RatingUpsertDto): RatingDto =
        put("/me/ratings", body)

    suspend fun deleteRating(entityType: String, entityId: String) {
        delete("/me/ratings") {
            url.parameters.append("entity_type", entityType)
            url.parameters.append("entity_id", entityId)
        }
    }

    suspend fun getMyDiary(): List<RatingDetailDto> = get("/me/diary")

    suspend fun getUserDiary(userId: String): List<RatingDetailDto> =
        get("/users/$userId/diary")

    suspend fun getFeed(limit: Int = 50): List<ActivityFeedDto> =
        get("/feed") {
            url.parameters.append("limit", limit.toString())
        }

    suspend fun getFollowing(): List<FollowingIdDto> = get("/me/following")

    suspend fun follow(userId: String) {
        postEmpty("/me/following/$userId")
    }

    suspend fun unfollow(userId: String) {
        delete("/me/following/$userId")
    }

    suspend fun isFollowing(userId: String): List<FollowCheckDto> =
        get("/me/following/$userId")

    suspend fun getMyLists(): List<MusicListDto> = get("/me/lists")

    suspend fun createList(body: CreateListRequestDto): MusicListDto =
        post("/me/lists", body)

    suspend fun deleteList(listId: String) {
        delete("/me/lists/$listId")
    }

    suspend fun getListItems(listId: String): List<ListItemDetailDto> =
        get("/lists/$listId/items")

    suspend fun addListItem(listId: String, body: AddListItemRequestDto): ListItemDetailDto =
        post("/me/lists/$listId/items", body)

    suspend fun removeListItem(listId: String, entityType: String, entityId: String) {
        delete("/me/lists/$listId/items") {
            url.parameters.append("entity_type", entityType)
            url.parameters.append("entity_id", entityId)
        }
    }

    suspend fun getPendingSubmissions(): List<SubmissionDto> =
        get("/admin/moderation/pending")

    suspend fun getMySubmissions(): List<SubmissionDto> = get("/me/submissions")

    suspend fun moderate(type: String, id: String, body: ModerationActionDto): SubmissionDto =
        patch("/admin/moderation/$type/$id", body)

    suspend fun setUserAdmin(username: String, makeAdmin: Boolean): ProfileDto =
        patch("/admin/users/${username.trim()}", SetAdminRequestDto(makeAdmin))

    suspend fun uploadAvatar(bytes: ByteArray, contentType: String, fileName: String): UploadResponseDto =
        uploadFile("/me/avatar", bytes, contentType, fileName)

    suspend fun uploadArtistImage(
        artistId: String,
        bytes: ByteArray,
        contentType: String,
        fileName: String
    ): UploadResponseDto =
        uploadFile("/catalog/artists/$artistId/image", bytes, contentType, fileName)

    suspend fun uploadAlbumCover(
        albumId: String,
        bytes: ByteArray,
        contentType: String,
        fileName: String
    ): UploadResponseDto =
        uploadFile("/catalog/albums/$albumId/cover", bytes, contentType, fileName)

    suspend fun uploadTrackCover(
        trackId: String,
        bytes: ByteArray,
        contentType: String,
        fileName: String
    ): UploadResponseDto =
        uploadFile("/catalog/tracks/$trackId/cover", bytes, contentType, fileName)

    suspend fun uploadListCover(
        listId: String,
        bytes: ByteArray,
        contentType: String,
        fileName: String
    ): UploadResponseDto =
        uploadFile("/me/lists/$listId/cover", bytes, contentType, fileName)

    private suspend inline fun <reified T> uploadFile(
        path: String,
        bytes: ByteArray,
        contentType: String,
        fileName: String
    ): T {
        val response = client.post("${BuildConfig.API_BASE_URL}$path") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            "file",
                            bytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, contentType)
                                append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            }
                        )
                    }
                )
            )
        }
        return decode(response)
    }

    private suspend inline fun <reified T> get(path: String, crossinline block: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {}): T {
        val response = client.get("${BuildConfig.API_BASE_URL}$path", block)
        return decode(response)
    }

    private suspend inline fun <reified T> post(path: String, body: Any): T {
        val response = client.post("${BuildConfig.API_BASE_URL}$path") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        return decode(response)
    }

    private suspend fun postEmpty(path: String) {
        val response = client.post("${BuildConfig.API_BASE_URL}$path")
        ensureSuccess(response)
    }

    private suspend inline fun <reified T> postPublic(path: String, body: Any): T {
        val response = client.post("${BuildConfig.API_BASE_URL}$path") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        return decode(response)
    }

    private suspend inline fun <reified T> put(path: String, body: Any): T {
        val response = client.put("${BuildConfig.API_BASE_URL}$path") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        return decode(response)
    }

    private suspend inline fun <reified T> patch(path: String, body: Any): T {
        val response = client.patch("${BuildConfig.API_BASE_URL}$path") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        return decode(response)
    }

    private suspend fun delete(path: String, block: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {}) {
        val response = client.delete("${BuildConfig.API_BASE_URL}$path", block)
        ensureSuccess(response)
    }

    private suspend inline fun <reified T> decode(response: HttpResponse): T {
        ensureSuccess(response)
        return response.body()
    }

    private suspend fun ensureSuccess(response: HttpResponse) {
        if (response.status.isSuccess()) return
        val raw = runCatching { response.bodyAsText() }.getOrDefault("")
        val detail = runCatching {
            kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString<ApiErrorDto>(raw)
                .detail
        }.getOrNull()
        throw ApiException(
            statusCode = response.status.value,
            message = detail ?: raw.ifBlank { response.status.description }
        )
    }
}
