package com.example.trackrate.domain.model

/** Lista personalizada de música del usuario. */
data class MusicList(
    val id: String,
    val title: String,
    val description: String?,
    val isPublic: Boolean
)

/** Elemento dentro de una lista con título resuelto. */
data class ListItemDetail(
    val listId: String,
    val entityType: CatalogType,
    val entityId: String,
    val position: Int,
    val title: String,
    val subtitle: String?
)
