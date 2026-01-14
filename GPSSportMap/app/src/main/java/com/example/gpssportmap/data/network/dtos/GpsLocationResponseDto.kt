package com.example.gpssportmap.data.network.dtos

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GpsLocationResponseDto(
    val id: String,
    val recordedAt: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double,
    val verticalAccuracy: Float,
    val appUserId: String,
    val gpsSessionId: String,
    val gpsLocationTypeId: String
)
