package com.example.gpssportmap.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.gpssportmap.data.db.dao.GpsLocationTypeDao
import com.example.gpssportmap.data.db.dao.GpsLocationsDao
import com.example.gpssportmap.data.db.dao.GpsSessionTypeDao
import com.example.gpssportmap.data.db.dao.GpsSessionsDao
import com.example.gpssportmap.data.db.entities.GpsLocationTypeEntity
import com.example.gpssportmap.data.db.entities.GpsLocationsEntity
import com.example.gpssportmap.data.db.entities.GpsSessionTypeEntity
import com.example.gpssportmap.data.db.entities.GpsSessionsEntity

@Database(
    entities = [
        GpsSessionsEntity::class,
        GpsLocationsEntity::class,
        GpsSessionTypeEntity::class,
        GpsLocationTypeEntity::class
    ],
    version = 21,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gpsSessionsDao(): GpsSessionsDao
    abstract fun gpsLocationsDao(): GpsLocationsDao
    abstract fun gpsSessionTypeDao(): GpsSessionTypeDao
    abstract fun gpsLocationTypeDao(): GpsLocationTypeDao
}
