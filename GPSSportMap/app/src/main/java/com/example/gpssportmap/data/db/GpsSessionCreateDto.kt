package com.example.gpssportmap.data.db

data class GpsSessionCreateDto(
    val name: String,
    val description: String?,
    val gpsSessionTypeId: String,
    val recordedAt: String,
    val paceMin: Double?,
    val paceMax: Double?
)
