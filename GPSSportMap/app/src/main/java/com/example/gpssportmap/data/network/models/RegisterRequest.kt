package com.example.gpssportmap.data.network.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String
)