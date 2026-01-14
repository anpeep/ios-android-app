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

    // This field will hold the nested JSON string: "{\"en\": \"Running - easy\",...}"
    // It's nullable because it might not be present if the API sends the correct ID.
    val gpsSessionType: String? = null,

    // This field will hold the correct ID: "00000000-0000-..."
    // We use @Json to tell Moshi to look for the "gpsSessionTypeId" key in the JSON.
    // It's nullable because it might not be present if the API sends the bad data.
    @Json(name = "gpsSessionTypeId")
    val gpsSessionTypeIdValue: String? = null
) {
    /**
     * This helper function provides the correct gpsSessionTypeId regardless of
     * which format the API decided to send. It prioritizes the correct field
     * and falls back to a hardcoded value if it finds the bad field.
     */
    fun getCorrectSessionTypeId(): String {
        // 1. If the correct field exists, use it. This is the best case.
        if (gpsSessionTypeIdValue != null) {
            return gpsSessionTypeIdValue
        }

        // 2. If the bad field exists, try to figure out the ID from its content.
        //    This is fragile and depends on hardcoded strings.
        if (gpsSessionType != null) {
            if (gpsSessionType.contains("Running - easy")) {
                return "00000000-0000-0000-0000-000000000001"
            }
            // Add other 'else if' conditions here for other sport types
            // else if (gpsSessionType.contains("Cycling")) { ... }
        }

        // 3. Fallback if neither field provides a usable ID.
        //    Return a default or throw an error. Returning the ID for "Running - easy"
        //    is a safe default based on your logs.
        return "00000000-0000-0000-0000-000000000001"
    }
}
