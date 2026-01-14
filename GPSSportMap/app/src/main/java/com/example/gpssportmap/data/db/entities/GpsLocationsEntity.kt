package com.example.gpssportmap.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "GpsLocations",
    foreignKeys = [
        ForeignKey(
            entity = GpsLocationTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["gpsLocationTypeId"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = GpsSessionsEntity::class,

            parentColumns = ["id"],
            childColumns = ["gpsSessionId"],
            onDelete = CASCADE
        )
    ],
    indices = [
        Index("gpsSessionId"),
        Index("gpsLocationTypeId"),
        Index("synced")
    ]
)
data class GpsLocationsEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    val id: String?,
    val recordedAt: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double,
    val verticalAccuracy: Float,
    val gpsSessionId: String,
    val gpsLocationTypeId: String,
    val appUserId: String,
    val synced: Boolean = false
)