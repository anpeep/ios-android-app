package com.example.gpssportmap.utils

object Utils {
    fun formatTime(sec: Long): String {
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return "%d:%02d:%02d".format(h, m, s)
    }


    fun formatPace(paceMinPerKm: Double): String {
        if (paceMinPerKm.isNaN() || paceMinPerKm.isInfinite()) return "--:--"
        val min = paceMinPerKm.toInt()
        val sec = ((paceMinPerKm - min) * 60).toInt()
        return "%d:%02d".format(min, sec)
    }
}