package com.example.gpssportmap.data.network.dtos

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GpsSessionsCreateDto(
    val name: String,
    val description: String,
    val gpsSessionTypeId: String,
    val recordedAt: String,
    val paceMin: Double?,
    val paceMax: Double?
)