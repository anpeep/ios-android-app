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
    gpsLocationTypeId: String, 
    appUserId: String         
): GpsLocationsEntity =
    GpsLocationsEntity(
        id = "", 
        recordedAt = recordedAt,
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        altitude = altitude,
        verticalAccuracy = verticalAccuracy,
        gpsSessionId = gpsSessionId,
        gpsLocationTypeId = gpsLocationTypeId, 
        appUserId = appUserId,
        synced = false 
    )

fun SessionResponseDto.toEntity(appUserId: String) =
    GpsSessionsEntity(
        id = id,
        name = name,
        description = description,
        recordedAt = recordedAt,
        duration = duration,
        speed = speed,
        distance = distance,
        climb = climb,
        descent = descent,
        paceMin = paceMin,
        paceMax = paceMax,
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
        speed = speed,
        distance = distance,
        climb = climb,
        descent = descent,
        paceMin = paceMin,
        paceMax = paceMax,
        appUserId = appUserId,
        gpsSessionTypeId = gpsSessionTypeId,
    )
