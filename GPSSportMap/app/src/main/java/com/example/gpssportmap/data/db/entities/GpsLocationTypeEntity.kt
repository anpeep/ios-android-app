package com.example.gpssportmap.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "GpsLocationTypes")
data class GpsLocationTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String
)