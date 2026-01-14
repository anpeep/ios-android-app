package com.example.gpssportmap.data.mappers

import android.location.Location
import com.example.gpssportmap.data.db.entities.GpsLocationTypeEntity
import com.example.gpssportmap.data.db.entities.GpsLocationsEntity
import com.example.gpssportmap.data.db.entities.GpsSessionTypeEntity
import com.example.gpssportmap.data.db.entities.GpsSessionsEntity
import com.example.gpssportmap.data.network.dtos.GpsLocationCreateDto
import com.example.gpssportmap.data.network.dtos.GpsLocationResponseDto
import com.example.gpssportmap.data.network.dtos.GpsLocationTypeResponseDto
import com.example.gpssportmap.data.network.dtos.GpsSessionTypeResponseDto
import com.example.gpssportmap.data.network.dtos.SessionCreateResponseDto
import com.example.gpssportmap.data.network.dtos.SessionResponseDto
import java.time.Instant

fun GpsLocationTypeResponseDto.toEntity() =
    GpsLocationTypeEntity(
        id = id,
        name = name,
        description = description
    )

fun GpsSessionTypeResponseDto.toEntity() =
    GpsSessionTypeEntity(
        id = id,
        name = name,
        description = description,
        paceMin = paceMin,
        paceMax = paceMax
    )

fun GpsLocationResponseDto.toEntity(): GpsLocationsEntity {
    return GpsLocationsEntity(
        id = id,
        recordedAt = recordedAt,
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        altitude = altitude,
        verticalAccuracy = verticalAccuracy,
        gpsLocationTypeId = gpsLocationTypeId,
        gpsSessionId = gpsSessionId,
        appUserId = appUserId
    )
}

fun Location.toGpsLocationCreateDto(
    locationTypeId: String
) = GpsLocationCreateDto(
    gpsLocationTypeId = locationTypeId,
    latitude = latitude,
    longitude = longitude,
    accuracy = accuracy,
    altitude = altitude,
    verticalAccuracy = verticalAccuracyMeters,
    recordedAt = Instant.now().toString()
)

fun GpsLocationCreateDto.toEntity(
    gpsSessionId: String,
    gpsLocationTypeId: String, // <-- 1. ADD this parameter
    appUserId: String         // 2. Make appUserId nullable, as it might not always exist
): GpsLocationsEntity =
    GpsLocationsEntity(
        id = "", // IMPORTANT: Let Room auto-generate the ID for local inserts. Don't use UUID here.
        recordedAt = recordedAt,
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        altitude = altitude,
        verticalAccuracy = verticalAccuracy,
        gpsSessionId = gpsSessionId,
        gpsLocationTypeId = gpsLocationTypeId, // 3. NOW it uses the parameter you passed in
        appUserId = appUserId,
        synced = false // It's good practice to set this explicitly for new local entries
    )

fun SessionResponseDto.toEntity(appUserId: String) =
    GpsSessionsEntity(
        id = id,
        name = name,
        description = description,
        recordedAt = recordedAt,
        duration = duration,
        speed = speed.toDouble(),
        distance = distance.toDouble(),
        climb = climb.toDouble(),
        descent = descent.toDouble(),
        paceMin = paceMin.toDouble(),
        paceMax = paceMax.toDouble(),
        gpsSessionTypeId = this.getCorrectSessionTypeId(),
        appUserId = appUserId
    )

fun SessionCreateResponseDto.toEntity(): GpsSessionsEntity =
    GpsSessionsEntity(
        id = id,
        name = name,
        description = description,
        recordedAt = recordedAt,
        duration = duration,
        speed = speed.toDouble(),
        distance = distance.toDouble(),
        climb = climb.toDouble(),
        descent = descent.toDouble(),
        paceMin = paceMin.toDouble(),
        paceMax = paceMax.toDouble(),
        appUserId = appUserId,
        gpsSessionTypeId = gpsSessionTypeId,
    )
