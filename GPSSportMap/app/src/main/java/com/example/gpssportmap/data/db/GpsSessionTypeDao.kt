package com.example.gpssportmap.data.db;

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Dao
interface GpsSessionTypeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(types: List<GpsSessionTypeEntity>)

    @Query("SELECT * FROM GpsSessionType")
    fun getAll(): Flow<List<GpsSessionTypeEntity>>
}