package com.example.trackrate.domain.model

enum class CatalogType { ARTIST, ALBUM, TRACK }

enum class ModerationStatus { PENDING, APPROVED, REJECTED }

enum class ContributorRole(val apiValue: String, val label: String) {
    PRODUCER("producer", "Productor"),
    FEATURED_ARTIST("featured_artist", "Artista invitado"),
    COMPOSER("composer", "Compositor"),
    LYRICIST("lyricist", "Letrista"),
    ENGINEER("engineer", "Ingeniero de sonido"),
    MIXER("mixer", "Mezcla"),
    MASTERING("mastering", "Masterización"),
    OTHER("other", "Otro");

    companion object {
        fun fromApiValue(value: String): ContributorRole =
            entries.firstOrNull { it.apiValue == value } ?: OTHER
    }
}

data class CatalogContributor(
    val artistId: String,
    val artistName: String,
    val role: String,
    val roleLabel: String,
    val notes: String?
)

data class CatalogSample(
    val trackId: String,
    val title: String,
    val artistName: String,
    val albumTitle: String?,
    val notes: String?
)

data class ContributorDraft(
    val artistId: String,
    val role: ContributorRole,
    val notes: String?
)

data class SampleDraft(
    val sampledTrackId: String,
    val notes: String?
)

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
    val durationMs: Int?,
    val label: String? = null,
    val contributors: List<CatalogContributor> = emptyList(),
    val samples: List<CatalogSample> = emptyList()
)
