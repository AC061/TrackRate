package com.example.trackrate.domain.model

/** Valoración del usuario actual sobre una entidad. */
data class Rating(
    val id: String,
    val rating: Double,
    val review: String?,
    val listenedAt: String?
)

/** Promedio y número de valoraciones de la comunidad para una entidad. */
data class RatingStats(
    val average: Double,
    val count: Int
)

/** Entrada del diario: una valoración con el título de la entidad resuelto. */
data class DiaryEntry(
    val id: String,
    val entityType: CatalogType,
    val entityId: String,
    val title: String,
    val subtitle: String?,
    val rating: Double,
    val review: String?,
    val listenedAt: String?
)
