package com.example.trackrate.data.repository

import com.example.trackrate.data.remote.TrackRateApi
import com.example.trackrate.data.remote.dto.AddListItemRequestDto
import com.example.trackrate.data.remote.dto.CreateListRequestDto
import com.example.trackrate.data.remote.dto.toApiType
import com.example.trackrate.domain.model.CatalogType
import com.example.trackrate.domain.model.ListItemDetail
import com.example.trackrate.domain.model.MusicList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListRepository @Inject constructor(
    private val api: TrackRateApi,
    private val authRepository: AuthRepository
) {

    suspend fun getMyLists(): List<MusicList> {
        authRepository.currentUserId ?: return emptyList()
        return api.getMyLists().map { it.toDomain() }
    }

    suspend fun createList(title: String, description: String?, isPublic: Boolean): MusicList {
        requireNotNull(authRepository.currentUserId) { "Sesión no válida" }
        return api.createList(
            CreateListRequestDto(
                title = title.trim(),
                description = description?.trim()?.ifBlank { null },
                isPublic = isPublic
            )
        ).toDomain()
    }

    suspend fun deleteList(listId: String) {
        api.deleteList(listId)
    }

    suspend fun getListItems(listId: String): List<ListItemDetail> = try {
        api.getListItems(listId).map { it.toDomain() }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun addItem(listId: String, type: CatalogType, entityId: String) {
        api.addListItem(
            listId,
            AddListItemRequestDto(
                entityType = type.toApiType(),
                entityId = entityId
            )
        )
    }

    suspend fun removeItem(listId: String, type: CatalogType, entityId: String) {
        api.removeListItem(listId, type.toApiType(), entityId)
    }
}
