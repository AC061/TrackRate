package com.example.trackrate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_catalog")
data class CachedCatalogEntity(
    @PrimaryKey(autoGenerate = true) val pk: Long = 0,
    val queryKey: String,
    val id: String,
    val type: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val year: Int?,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_diary")
data class CachedDiaryEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val title: String,
    val subtitle: String?,
    val rating: Double,
    val review: String?,
    val listenedAt: String?,
    val cachedAt: Long = System.currentTimeMillis()
)
