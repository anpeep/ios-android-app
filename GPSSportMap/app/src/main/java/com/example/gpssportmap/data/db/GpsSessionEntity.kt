package com.example.gpssportmap.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "GpsSession",
    foreignKeys = [ForeignKey(
        entity = GpsSessionTypeEntity::class,
        parentColumns = ["id"],
        childColumns = ["gpsSessionTypeId"],
        onDelete = ForeignKey.Companion.CASCADE
    )]
)
@JsonClass(generateAdapter = true)
data class GpsSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String?,
    val recordedAt: String,
    val duration: Double? = 0.0,
    val speed: Double? = 0.0,
    val distance: Double? = 0.0,
    val climb: Double? = 0.0,
    val descent: Double? = 0.0,
    val paceMin: Double? = 0.0,
    val paceMax: Double? = 0.0,
    val gpsSessionTypeId: String? = null,
    val isActive: Boolean = false

)