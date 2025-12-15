package com.example.gpssportmap.data.network.models
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JwtResponse(
    val token: String,
    val status: String,
    val firstName: String,
    val lastName: String
)