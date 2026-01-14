package com.example.gpssportmap.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.example.gpssportmap.data.db.entities.GpsSessionsEntity
import com.example.gpssportmap.data.db.entities.SessionWithLocations
import com.example.gpssportmap.utils.Utils
import kotlinx.coroutines.flow.Flow

@Dao
interface GpsSessionsDao {
    @Upsert
    suspend fun upsert(session: GpsSessionsEntity)

    @Update
    suspend fun update(session: GpsSessionsEntity)

    @Query("SELECT * FROM GpsSessions WHERE id = :id")
    suspend fun getSessionById(id: String): GpsSessionsEntity

    @Query("SELECT * FROM GpsSessions WHERE id = :sessionId")
    fun getSessionByIdFlow(sessionId: String): Flow<GpsSessionsEntity?>

    @Query("SELECT * FROM GpsSessions WHERE id = :sessionId LIMIT 1")
    fun getSession(sessionId: String): Flow<GpsSessionsEntity?>

    @Query(
        """
UPDATE GpsSessions
SET paceMin = :paceMin,
    paceMax = :paceMax
WHERE id = :sessionId
"""
    )
    suspend fun updatePaceRange(
        sessionId: String,
        paceMin: Double,
        paceMax: Double
    )

    @Query(
        """
SELECT paceMin, paceMax
FROM GpsSessions
WHERE id = :sessionId
"""
    )
    fun getSettings(sessionId: String): Flow<Utils.SessionColorSettings>

    @Query(
        """
    UPDATE GpsSessions
    SET duration = :duration,
        distance = :distance,
        speed = :speed
    WHERE id = :sessionId
"""
    )
    suspend fun updateStats(
        sessionId: String,
        duration: Double,
        distance: Double,
        speed: Double
    )

    @Query("SELECT * FROM GpsSessions")
    fun getAllSessions(): Flow<List<GpsSessionsEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM GpsSessions WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    @Query("UPDATE GpsSessions SET name = :name WHERE id = :id")
    suspend fun updateSessionName(id: String, name: String)

    @Transaction
    @Query("SELECT * FROM gpssessions WHERE id = :sessionId")
    fun getSessionWithLocations(sessionId: String): Flow<SessionWithLocations?>

    @Query("DELETE FROM GpsSessions WHERE id = :id")
    suspend fun delete(id: String)
}
