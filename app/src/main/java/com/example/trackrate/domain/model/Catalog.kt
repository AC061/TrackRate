package com.example.trackrate.domain.model

enum class CatalogType { ARTIST, ALBUM, TRACK }

enum class ModerationStatus { PENDING, APPROVED, REJECTED }

/** Entrada del catálogo con su estado de moderación (cola admin y "Mis envíos"). */
data class CatalogSubmission(
    val id: String,
    val type: CatalogType,
    val title: String,
    val subtitle: String?,
    val status: ModerationStatus,
    val rejectionReason: String?
)

/** Elemento ligero para listas de búsqueda y selección. */
data class CatalogItem(
    val id: String,
    val type: CatalogType,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val year: Int?
)

/** Detalle completo de una entidad del catálogo aprobado. */
data class CatalogDetail(
    val id: String,
    val type: CatalogType,
    val title: String,
    val subtitle: String?,
    val extra: String?,
    val description: String?,
    val imageUrl: String?,
    val year: Int?,
    val durationMs: Int?
)
