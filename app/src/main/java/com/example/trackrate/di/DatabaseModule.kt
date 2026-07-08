package com.example.trackrate.di

import android.content.Context
import androidx.room.Room
import com.example.trackrate.data.local.TrackRateDatabase
import com.example.trackrate.data.local.dao.CatalogCacheDao
import com.example.trackrate.data.local.dao.DiaryCacheDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TrackRateDatabase =
        Room.databaseBuilder(
            context,
            TrackRateDatabase::class.java,
            "trackrate_cache.db"
        ).build()

    @Provides
    @Singleton
    fun provideCatalogCacheDao(db: TrackRateDatabase): CatalogCacheDao = db.catalogCacheDao()

    @Provides
    @Singleton
    fun provideDiaryCacheDao(db: TrackRateDatabase): DiaryCacheDao = db.diaryCacheDao()
}
