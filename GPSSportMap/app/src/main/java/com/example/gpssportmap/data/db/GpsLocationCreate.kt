package com.example.gpssportmap.data.db

data class GpsLocationCreate(
    val recordedAt: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double?,
    val altitude: Double?,
    val verticalAccuracy: Double?,
    val gpsSessionId: String,
    val gpsLocationTypeId: String
)
