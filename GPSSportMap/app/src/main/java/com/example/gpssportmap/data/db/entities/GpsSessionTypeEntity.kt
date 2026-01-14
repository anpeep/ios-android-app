package com.example.gpssportmap.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "GpsSessionType")
data class GpsSessionTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val paceMin: Double,
    val paceMax: Double
)