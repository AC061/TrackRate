package com.example.trackrate.domain.model

data class RecordLabel(
    val id: String,
    val name: String
)

data class TopRatedTrack(
    val id: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val averageRating: Double,
    val ratingCount: Int
)
