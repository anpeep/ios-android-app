package com.example.gpssportmap.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
data class SessionId(val id: String?)
@Dao
interface GpsSessionDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSession(s: GpsSessionEntity)
    @Query("SELECT * FROM GpsSession WHERE id = :id") suspend fun getSessionById(id: String): GpsSessionEntity?
    @Query("SELECT * FROM GpsSession") fun getAllSessions(): Flow<List<GpsSessionEntity>>

    @Query("SELECT * FROM GpsSession WHERE id = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: String): GpsSessionEntity?

    // --- FIX 2: Change the function to use the new data class ---
    @Query("SELECT id FROM GpsSession WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSessionId(): SessionId? // Return the wrapper class instead of String?

    @Query("UPDATE GpsSession SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE GpsSession SET name = :name, description = :description WHERE id = :sessionId")
    suspend fun updateSessionDetails(sessionId: String, name: String, description: String?)

    @Update
    suspend fun update(session: GpsSessionEntity)

    @Query("DELETE FROM GpsSession WHERE id = :sessionId")
    suspend fun delete(sessionId: String)
}