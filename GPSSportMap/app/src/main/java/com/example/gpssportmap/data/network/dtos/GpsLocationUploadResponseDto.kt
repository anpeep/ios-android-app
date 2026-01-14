package com.example.gpssportmap.data.network.dtos

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GpsLocationUploadResponseDto(
    val locationsAdded: Int,
    val locationsReceived: Int,
    val gpsSessionId: String
)
