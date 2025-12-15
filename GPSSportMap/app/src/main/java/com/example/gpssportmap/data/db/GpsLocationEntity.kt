package com.example.gpssportmap.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import java.util.UUID

@Entity(
    tableName = "GpsLocation",
    foreignKeys = [
        ForeignKey(
            entity = GpsSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["gpsSessionId"],
            onDelete = ForeignKey.Companion.CASCADE
        ),
        ForeignKey(
            entity = GpsLocationTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["gpsLocationTypeId"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ]
)
@JsonClass(generateAdapter = true)
data class GpsLocationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val recordedAt: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double?,
    val altitude: Double?,
    val verticalAccuracy: Double?,
    val gpsSessionId: String,
    val gpsLocationTypeId: String
)