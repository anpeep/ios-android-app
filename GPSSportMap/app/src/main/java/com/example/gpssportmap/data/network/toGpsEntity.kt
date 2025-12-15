package com.example.gpssportmap.data.network

import android.location.Location
import com.example.gpssportmap.data.db.GpsLocationEntity
import java.time.Instant
import java.util.UUID

fun Location.toGpsEntity(
        type: String,
        sessionId: String
    ): GpsLocationEntity {
        return GpsLocationEntity(
            id = UUID.randomUUID().toString(),
            gpsSessionId = sessionId,
            gpsLocationTypeId = type,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            accuracy = accuracy.toDouble(),
            recordedAt = Instant.now().toString(),
            verticalAccuracy = verticalAccuracyMeters.toDouble(),
        )
    }