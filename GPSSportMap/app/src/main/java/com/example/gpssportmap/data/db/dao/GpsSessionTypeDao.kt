package com.example.gpssportmap.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gpssportmap.data.db.entities.GpsSessionTypeEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface GpsSessionTypeDao {
    @Query("SELECT COUNT(*) FROM GpsSessionType")
    fun count(): Int

    @Query("SELECT * FROM GpsSessionType")
    fun getAllSessionTypes(): Flow<List<GpsSessionTypeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM GpsSessionType WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    @Query("SELECT * FROM GpsSessionType WHERE id = :id LIMIT 1")
    suspend fun getSessionTypeById(id: String): GpsSessionTypeEntity

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(types: List<GpsSessionTypeEntity>)
}
