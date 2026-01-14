package com.example.gpssportmap.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gpssportmap.data.db.AppDatabase
import com.example.gpssportmap.data.db.dao.GpsLocationTypeDao
import com.example.gpssportmap.data.db.dao.GpsLocationsDao
import com.example.gpssportmap.data.db.dao.GpsSessionTypeDao
import com.example.gpssportmap.data.db.dao.GpsSessionsDao
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
        ).fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA foreign_keys=ON")
                }
            })
            .build()
    }


    @Provides
    fun provideSessionsDao(db: AppDatabase): GpsSessionsDao =
        db.gpsSessionsDao()

    @Provides
    fun provideSessionTypeDao(db: AppDatabase): GpsSessionTypeDao =
        db.gpsSessionTypeDao()

    @Provides
    fun provideLocationDao(db: AppDatabase): GpsLocationsDao =
        db.gpsLocationsDao()

    @Provides
    fun provideLocationTypeDao(db: AppDatabase): GpsLocationTypeDao =
        db.gpsLocationTypeDao()
}
