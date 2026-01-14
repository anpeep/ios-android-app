package com.example.gpssportmap.data.network.dtos

data class GpsSessionTypeResponseDto(
    val id: String,
    val name: String,
    val description: String,
    val paceMin: Double,
    val paceMax: Double
)