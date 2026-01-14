package com.example.gpssportmap.data.network.dtos

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SessionCreateResponseDto(
    val id: String,
    val name: String,
    val description: String,
    val recordedAt: String,
    val duration: Double,   
    val speed: Double,      
    val distance: Double,   
    val climb: Double,      
    val descent: Double,    
    val paceMin: Double,    
    val paceMax: Double,    
    val gpsSessionTypeId: String,
    val appUserId: String
)
