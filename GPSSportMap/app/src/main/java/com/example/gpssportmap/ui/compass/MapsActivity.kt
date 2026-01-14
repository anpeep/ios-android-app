package com.example.gpssportmap.ui.compass

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Surface
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpssportmap.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {
    private lateinit var mMap: GoogleMap
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var magnetometer: Sensor
    private lateinit var compassImage: ImageView
    private var lastAccelerometer = FloatArray(3)
    private var lastMagnetometer = FloatArray(3)
    var orientationMode: MapOrientationMode = MapOrientationMode.NORTH

    private var currentDegree = 0f
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        compassImage = findViewById(R.id.imageViewCompass)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)!!
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    override fun onSensorChanged(event: SensorEvent) {
        val r = FloatArray(9)
        val i = FloatArray(9)

        if (SensorManager.getRotationMatrix(r, i, lastAccelerometer, lastMagnetometer)) {

            val outR = FloatArray(9)
            val rotation = windowManager.defaultDisplay.rotation

            when (rotation) {
                Surface.ROTATION_0 ->
                    SensorManager.remapCoordinateSystem(
                        r,
                        SensorManager.AXIS_X,
                        SensorManager.AXIS_Y,
                        outR
                    )

                Surface.ROTATION_90 ->
                    SensorManager.remapCoordinateSystem(
                        r,
                        SensorManager.AXIS_Y,
                        SensorManager.AXIS_MINUS_X,
                        outR
                    )

                Surface.ROTATION_180 ->
                    SensorManager.remapCoordinateSystem(
                        r,
                        SensorManager.AXIS_MINUS_X,
                        SensorManager.AXIS_MINUS_Y,
                        outR
                    )

                Surface.ROTATION_270 ->
                    SensorManager.remapCoordinateSystem(
                        r,
                        SensorManager.AXIS_MINUS_Y,
                        SensorManager.AXIS_X,
                        outR
                    )
            }

            val orientation = FloatArray(3)
            SensorManager.getOrientation(outR, orientation)

            val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
            val bearing = (azimuth + 360) % 360

            updateCompass(bearing)
        }
    }

    private fun updateCompass(bearing: Float) {
        val anim = RotateAnimation(
            currentDegree,
            -bearing,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        anim.duration = 150
        anim.fillAfter = true
        compassImage.startAnimation(anim)
        currentDegree = -bearing

        handleMapBearing(bearing)
    }

    private fun handleMapBearing(bearing: Float) {
        if (!::mMap.isInitialized) return

        when (orientationMode) {

            MapOrientationMode.COMPASS -> {
                mMap.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder(mMap.cameraPosition)
                            .bearing(bearing)
                            .build()
                    )
                )
            }

            MapOrientationMode.NORTH -> {
                mMap.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder(mMap.cameraPosition)
                            .bearing(0f)
                            .build()
                    )
                )
            }

            else -> Unit
        }
    }

    fun updateSensorState() {
        if (orientationMode == MapOrientationMode.COMPASS) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME)
            compassImage.visibility = View.VISIBLE
        } else {
            sensorManager.unregisterListener(this)
            compassImage.visibility = View.GONE
        }
    }

    @Preview(showBackground = true, name = "Interactive Preview")
    @Composable
    fun PreviewMapOrientationControls() {
        var selectedMode by remember { mutableStateOf(MapOrientationMode.COMPASS) }

        MapOrientationControls(
            modifier = Modifier.padding(16.dp),
            initialMode = selectedMode,
            onModeChange = { newMode ->
                orientationMode = newMode
                updateSensorState()
            },
            azimuth = TODO()
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}