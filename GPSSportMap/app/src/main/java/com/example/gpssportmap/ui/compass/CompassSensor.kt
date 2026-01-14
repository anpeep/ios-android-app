package com.example.gpssportmap.ui.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CompassSensor(context: Context) : SensorEventListener {
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accel = FloatArray(3)
    private val magnet = FloatArray(3)
    private val _azimuth = MutableStateFlow(0f)
    val azimuth: StateFlow<Float> get() = _azimuth
    private var lastAzimuth = 0f
    fun start() {
        sm.registerListener(
            this,
            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_UI
        )
        sm.registerListener(
            this,
            sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_UI
        )
    }

    fun stop() {
        sm.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> System.arraycopy(event.values, 0, accel, 0, 3)
            Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(event.values, 0, magnet, 0, 3)
        }
        val R = FloatArray(9)
        val I = FloatArray(9)
        if (SensorManager.getRotationMatrix(R, I, accel, magnet)) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(R, orientation)
            val az = Math.toDegrees(orientation[0].toDouble()).toFloat()

            // --- SUGGESTION: Apply a low-pass filter ---
            _azimuth.value = applyLowPassFilter((az + 360) % 360)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // --- SUGGESTION: Add this helper function ---
    private fun applyLowPassFilter(newAzimuth: Float): Float {
        val alpha = 0.4f // Smoothing factor (0.0 to 1.0). Lower values mean more smoothing.

        // Handle the 360/0 degree wrap-around
        val diff = newAzimuth - lastAzimuth
        if (kotlin.math.abs(diff) > 180) {
            if (diff > 0) {
                lastAzimuth += 360
            } else {
                lastAzimuth -= 360
            }
        }

        lastAzimuth += alpha * (newAzimuth - lastAzimuth)
        return lastAzimuth
    }
}