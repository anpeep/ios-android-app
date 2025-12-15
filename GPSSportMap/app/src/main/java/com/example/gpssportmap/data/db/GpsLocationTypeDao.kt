package com.example.gpssportmap.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GpsLocationTypeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(types: List<GpsLocationTypeEntity>)

    @Query("SELECT * FROM GpsLocationType")
    fun getAll(): Flow<List<GpsLocationTypeEntity>>
}