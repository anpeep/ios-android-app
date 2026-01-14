package com.example.gpssportmap.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "GpsSessions",
    foreignKeys = [
        ForeignKey(
            entity = GpsSessionTypeEntity::class, // <-- Parent Table
            parentColumns = ["id"],
            childColumns = ["gpsSessionTypeId"], // <-- Child Column in this table
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("gpsSessionTypeId")]
)

data class GpsSessionsEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val recordedAt: String,
    val duration: Double,
    val speed: Double,
    val distance: Double,
    val climb: Double,
    val descent: Double,
    val paceMin: Double,
    val paceMax: Double,
    val gpsSessionTypeId: String,
    val appUserId: String
)