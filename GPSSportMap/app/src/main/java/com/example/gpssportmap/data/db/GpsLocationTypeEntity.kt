package com.example.gpssportmap.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "GpsLocationType")
data class GpsLocationTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String
)