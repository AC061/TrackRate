package com.example.trackrate.data.local

import com.example.trackrate.data.local.entity.CachedCatalogEntity
import com.example.trackrate.data.local.entity.CachedDiaryEntity
import com.example.trackrate.domain.model.CatalogItem
import com.example.trackrate.domain.model.CatalogType
import com.example.trackrate.domain.model.DiaryEntry

fun CatalogItem.toCachedEntity(queryKey: String) = CachedCatalogEntity(
    queryKey = queryKey,
    id = id,
    type = type.name,
    title = title,
    subtitle = subtitle,
    imageUrl = imageUrl,
    year = year
)

fun CachedCatalogEntity.toCatalogItem() = CatalogItem(
    id = id,
    type = CatalogType.valueOf(type),
    title = title,
    subtitle = subtitle,
    imageUrl = imageUrl,
    year = year
)

fun DiaryEntry.toCachedEntity() = CachedDiaryEntity(
    id = id,
    entityType = entityType.name,
    entityId = entityId,
    title = title,
    subtitle = subtitle,
    rating = rating,
    review = review,
    listenedAt = listenedAt
)

fun CachedDiaryEntity.toDiaryEntry() = DiaryEntry(
    id = id,
    entityType = CatalogType.valueOf(entityType),
    entityId = entityId,
    title = title,
    subtitle = subtitle,
    rating = rating,
    review = review,
    listenedAt = listenedAt
)

fun catalogQueryKey(query: String, type: CatalogType?) =
    "${query.trim().lowercase()}|${type?.name ?: "ALL"}"
