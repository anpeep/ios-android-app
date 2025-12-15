package com.example.gpssportmap.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
@Database(
    entities = [
        GpsSessionEntity::class,
        GpsLocationEntity::class,
        GpsSessionTypeEntity::class,
        GpsLocationTypeEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gpsSessionDao(): GpsSessionDao
    abstract fun gpsLocationDao(): GpsLocationDao
    abstract fun gpsSessionTypeDao(): GpsSessionTypeDao
    abstract fun gpsLocationTypeDao(): GpsLocationTypeDao
}

