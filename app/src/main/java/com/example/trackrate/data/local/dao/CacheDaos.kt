package com.example.trackrate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.trackrate.data.local.entity.CachedCatalogEntity
import com.example.trackrate.data.local.entity.CachedDiaryEntity

@Dao
interface CatalogCacheDao {

    @Query("DELETE FROM cached_catalog WHERE queryKey = :queryKey")
    suspend fun clearQuery(queryKey: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedCatalogEntity>): List<Long>

    @Query("SELECT * FROM cached_catalog WHERE queryKey = :queryKey ORDER BY title COLLATE NOCASE")
    suspend fun getByQuery(queryKey: String): List<CachedCatalogEntity>
}

@Dao
interface DiaryCacheDao {

    @Query("DELETE FROM cached_diary")
    suspend fun clearAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedDiaryEntity>): List<Long>

    @Query("SELECT * FROM cached_diary ORDER BY cachedAt DESC")
    suspend fun getAll(): List<CachedDiaryEntity>
}
