package com.example.gpssportmap.utils

import com.example.gpssportmap.data.db.entities.GpsLocationsEntity
import java.time.LocalDateTime
import javax.inject.Inject

class GpxExporter @Inject constructor() {
    fun buildGpx(
        track: List<GpsLocationsEntity>,
        checkpoints: List<GpsLocationsEntity>,
        waypoints: List<GpsLocationsEntity>
    ): String {
        fun getLat(e: GpsLocationsEntity) = e.latitude
        fun getLon(e: GpsLocationsEntity) = e.longitude
        fun time(t: String) = "<time>$t</time>"
        val sb = StringBuilder()
        sb.append(
            """
        <?xml version="1.0" encoding="UTF-8"?>
        <gpx version="1.1" creator="GpsSportMap"
             xmlns="http://www.topografix.com/GPX/1/1">
        <trk>
            <name>${LocalDateTime.now()}</name>
            <trkseg>
    """.trimIndent()
        )
        track.forEach {
            sb.appendLine(
                """<trkpt lat="${getLat(it)}" lon="${getLon(it)}"><ele>${it.altitude}</ele>${
                    time(
                        it.recordedAt
                    )
                }</trkpt>"""
            )
        }
        sb.appendLine("</trkseg></trk>")
        checkpoints.forEach {
            sb.appendLine(
                """<wpt lat="${getLat(it)}" lon="${getLon(it)}"><name>CP</name>${
                    time(
                        it.recordedAt
                    )
                }</wpt>"""
            )
        }
        waypoints.forEach {
            sb.appendLine(
                """<wpt lat="${getLat(it)}" lon="${getLon(it)}"><name>WP</name>${
                    time(
                        it.recordedAt
                    )
                }</wpt>"""
            )
        }
        sb.append("</gpx>")
        return sb.toString()
    }
}