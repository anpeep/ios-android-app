package com.example.gpssportmap.data.network.dtos

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SessionCreateResponseDto(
    val id: String,
    val name: String,
    val description: String,
    val recordedAt: String,
    val duration: Double,   // <- was Int
    val speed: Double,      // <- was Int
    val distance: Double,   // <- was Int
    val climb: Double,      // <- was Int
    val descent: Double,    // <- was Int
    val paceMin: Double,    // <- was Int
    val paceMax: Double,    // <- was Int
    val gpsSessionTypeId: String,
    val appUserId: String
)
