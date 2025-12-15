package com.example.gpssportmap.data.network

import android.location.Location
import com.example.gpssportmap.data.db.GpsLocationEntity
import java.time.Instant

fun Location.toGpsLocationEntity(
    sessionId: String,
    typeId: String
): GpsLocationEntity =
    GpsLocationEntity(
        recordedAt = Instant.now().toString(),
        latitude = latitude,
        longitude = longitude,
        accuracy = if (hasAccuracy()) accuracy.toDouble() else null,
        altitude = if (hasAltitude()) altitude else null,
        verticalAccuracy = if (hasVerticalAccuracy())
            verticalAccuracyMeters.toDouble()
        else null,
        gpsSessionId = sessionId,
        gpsLocationTypeId = typeId
    )
