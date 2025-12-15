package com.example.gpssportmap.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GpsLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(location: GpsLocationEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(locations: List<GpsLocationEntity>)
    @Query("SELECT * FROM GpsLocation WHERE gpsSessionId = :sessionId")
    fun getLocationsForSession(sessionId: String): Flow<List<GpsLocationEntity>>
    @Query("DELETE FROM GpsLocation WHERE gpsSessionId = :sessionId") suspend fun deleteForSession(sessionId: String)
}