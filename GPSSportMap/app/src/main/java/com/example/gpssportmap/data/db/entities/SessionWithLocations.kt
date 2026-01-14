package com.example.gpssportmap.data.db.entities

import androidx.room.Embedded
import androidx.room.Relation

data class SessionWithLocations(
    @Embedded val session: GpsSessionsEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "gpsSessionId"
    )
    val locations: List<GpsLocationsEntity>
)
