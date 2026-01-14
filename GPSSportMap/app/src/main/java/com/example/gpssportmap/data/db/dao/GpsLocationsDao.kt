package com.example.gpssportmap.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.gpssportmap.data.db.entities.GpsLocationsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GpsLocationsDao {
    @Query("DELETE FROM GpsLocations WHERE gpsSessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entity: List<GpsLocationsEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: GpsLocationsEntity)

    @Upsert
    suspend fun upsertAll(locations: List<GpsLocationsEntity>)

    @Query(
        """
    SELECT * FROM GpsLocations 
    WHERE gpsSessionId = :sessionId 
    ORDER BY recordedAt ASC
"""
    )
    fun getLocationsForSession(sessionId: String): Flow<List<GpsLocationsEntity>>

    @Query("SELECT * FROM GpsLocations WHERE gpsSessionId = :sessionId AND gpsLocationTypeId = :typeId ORDER BY recordedAt ASC")
    fun getLocationsByType(sessionId: String, typeId: String): Flow<List<GpsLocationsEntity>>

    @Query(
        """
    SELECT * FROM GpsLocations
    ORDER BY recordedAt DESC
    LIMIT 1
"""
    )
    suspend fun getLastLocation(): GpsLocationsEntity?

    @Query("SELECT * FROM GpsLocations WHERE synced = 0")
    suspend fun getUnsynced(): List<GpsLocationsEntity>

    @Query("UPDATE GpsLocations SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

}
