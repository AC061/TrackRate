package com.example.trackrate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.trackrate.data.local.dao.CatalogCacheDao
import com.example.trackrate.data.local.dao.DiaryCacheDao
import com.example.trackrate.data.local.entity.CachedCatalogEntity
import com.example.trackrate.data.local.entity.CachedDiaryEntity

@Database(
    entities = [CachedCatalogEntity::class, CachedDiaryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TrackRateDatabase : RoomDatabase() {
    abstract fun catalogCacheDao(): CatalogCacheDao
    abstract fun diaryCacheDao(): DiaryCacheDao
}
