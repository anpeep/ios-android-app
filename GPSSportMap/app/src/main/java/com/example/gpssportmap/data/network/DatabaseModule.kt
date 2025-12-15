package com.example.gpssportmap.data.network

import android.content.Context
import androidx.room.Room
import com.example.gpssportmap.data.db.AppDatabase
import com.example.gpssportmap.data.db.GpsLocationDao
import com.example.gpssportmap.data.db.GpsLocationTypeDao
import com.example.gpssportmap.data.db.GpsSessionDao
import com.example.gpssportmap.data.db.GpsSessionTypeDao
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
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "sportmap.db"
        ).build()
    }

    @Provides
    fun provideSessionDao(db: AppDatabase): GpsSessionDao =
        db.gpsSessionDao()

    @Provides
    fun provideLocationDao(db: AppDatabase): GpsLocationDao =
        db.gpsLocationDao()

    @Provides
    fun provideSessionTypeDao(db: AppDatabase): GpsSessionTypeDao =
        db.gpsSessionTypeDao()

    @Provides
    fun provideLocationTypeDao(db: AppDatabase): GpsLocationTypeDao =
        db.gpsLocationTypeDao()
}
