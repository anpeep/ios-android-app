package com.example.gpssportmap.data.network.dtos

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SessionResponseDto(
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
    val gpsLocationsCount: Int,
    val userFirstLastName: String,
    val gpsSessionType: String? = null,
    @Json(name = "gpsSessionTypeId")
    val gpsSessionTypeIdValue: String? = null
) {
    fun getCorrectSessionTypeId(): String {
       
        if (gpsSessionTypeIdValue != null) {
            return gpsSessionTypeIdValue
        }
       
        if (gpsSessionType != null) {
            if (gpsSessionType.contains("Running - easy")) {
                return "00000000-0000-0000-0000-000000000001"
            }
        }
        return "00000000-0000-0000-0000-000000000001"
    }
}
