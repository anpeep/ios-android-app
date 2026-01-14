package com.example.gpssportmap.utils

import android.location.Location
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.google.android.gms.maps.model.LatLng

object Utils {
    fun formatTime(sec: Double): String {
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return "%d:%02d:%02d".format(h.toInt(), m.toInt(), s.toInt())

    }

    fun formatPace(paceMinPerKm: Double): String {
        if (paceMinPerKm.isNaN() || paceMinPerKm.isInfinite()) return "--:--"
        val min = paceMinPerKm.toInt()
        val sec = ((paceMinPerKm - min) * 60).toInt()
        return "%d:%02d".format(min, sec)
    }

    data class SessionColorSettings(
        val paceMin: Double = 0.5,
        val paceMax: Double = 100.0  // Default fastest pace
    )

    fun SessionColorSettings.toSessionColorSettings(): SessionColorSettings {
        return SessionColorSettings(
            paceMin = this.paceMin,
            paceMax = this.paceMax
        )
    }

    fun SessionColorSettings.toGpsSettings(): SessionColorSettings {
        return SessionColorSettings(
            paceMin = this.paceMin,
            paceMax = this.paceMax
        )
    }

    fun paceToColor(pace: Double, min: Double, max: Double): Color {
        val t = ((pace - min) / (max - min)).coerceIn(0.0, 1.0)
        return lerp(
            Color.Green, // fast
            Color.Red,   // slow
            t.toFloat()
        )
    }

    fun computeSegmentPaces(points: List<TrackPoint>): List<Double> {
        return points.windowed(2, 1).map { (p1, p2) ->
            val distanceMeters = FloatArray(1).also {
                Location.distanceBetween(
                    p1.latLng.latitude,
                    p1.latLng.longitude,
                    p2.latLng.latitude,
                    p2.latLng.longitude,
                    it
                )
            }[0]

            val timeSec = (p2.timeMillis - p1.timeMillis) / 1000.0

            if (timeSec <= 0 || distanceMeters < 2) {
                Double.MAX_VALUE
            } else {
                val speedMps = distanceMeters / timeSec
                (1000.0 / speedMps) / 60.0 // min/km
            }
        }
    }


    data class TrackPoint(
        val latLng: LatLng,
        val timeMillis: Long
    )

}