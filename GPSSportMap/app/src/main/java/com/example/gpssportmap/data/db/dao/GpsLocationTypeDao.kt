package com.example.gpssportmap.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gpssportmap.data.db.entities.GpsLocationTypeEntity

@Dao
interface GpsLocationTypeDao {
    @Query("SELECT COUNT(*) FROM GpsLocationTypes")
    suspend fun count(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM GpsLocationTypes WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(sessionType: GpsLocationTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(types: List<GpsLocationTypeEntity>)
}

