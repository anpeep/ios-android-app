package com.example.gpssportmap.data.mappers

import com.example.gpssportmap.data.db.entities.GpsLocationsEntity
import com.example.gpssportmap.data.network.dtos.GpsLocationCreateDto

fun GpsLocationsEntity.toDto(): GpsLocationCreateDto {
    return GpsLocationCreateDto(
        latitude = this.latitude,
        longitude = this.longitude,
        altitude = this.altitude,
        recordedAt = this.recordedAt,
        accuracy = this.accuracy,
        verticalAccuracy = this.verticalAccuracy,
        gpsLocationTypeId = this.gpsLocationTypeId,
    )
}