package com.example.gpssportmap.domain.model

import android.location.Location
import android.util.Log
import com.example.gpssportmap.data.db.GpsLocationEntity
import com.example.gpssportmap.data.db.GpsSessionCreateDto
import com.example.gpssportmap.data.network.toGpsEntity
import com.example.gpssportmap.data.network.toGpsLocationEntity
import com.example.gpssportmap.data.repository.SessionRepository
import com.example.gpssportmap.utils.C
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionTracker @Inject constructor(
    private val repository: SessionRepository
) {
    private val _isTracking = MutableStateFlow(false)
    val isTracking = _isTracking.asStateFlow()

    private val _trackPoints = MutableStateFlow<List<Location>>(emptyList())
    val trackPoints = _trackPoints.asStateFlow()

    private val _checkpoints = MutableStateFlow<List<GpsLocationEntity>>(emptyList())
    val checkpoints = _checkpoints.asStateFlow()

    private val _waypoint = MutableStateFlow<Location?>(null)
    val waypoint = _waypoint.asStateFlow()

    private val _distance = MutableStateFlow(0.0)
    val distance = _distance.asStateFlow()

    private val _elapsed = MutableStateFlow(0L)
    val elapsed = _elapsed.asStateFlow()

    private var timerJob: Job? = null

    private var lastLocation: Location? = null

    private val _totalDistance = MutableStateFlow(0.0)
    val totalDistanceMeters = _totalDistance.asStateFlow()
    private val _currentSessionId = MutableStateFlow<UUID?>(null)
    suspend fun onLocation(location: Location) {
        Log.d("SessionTracker", "Received location: ${location.latitude}, ${location.longitude}")

        // Always update UI
        _trackPoints.value = _trackPoints.value + location

        // Only calculate distance & persist when tracking
        if (!_isTracking.value) {
            lastLocation = location
            return
        }

        lastLocation?.let {
            _distance.value += it.distanceTo(location)
        }
        lastLocation = location

        val sessionId = repository.getActiveSessionId() ?: return
        repository.addLocation(
            location.toGpsEntity(
                type = C.LOCATION_TYPE_TRACK,
                sessionId = sessionId
            )
        )
    }
    suspend fun startSession(
        name: String,
        description: String?,
        sessionTypeId: String
    ) {
        val dto = GpsSessionCreateDto(
            name = name,
            description = description,
            gpsSessionTypeId = sessionTypeId,
            recordedAt = Instant.now().toString(),
            paceMin = null,
            paceMax = null
        )

        val created = repository.startSession(dto)

        _currentSessionId.value = UUID.fromString(created.id.toString())

        _isTracking.value = true
        _distance.value = 0.0
        _elapsed.value = 0L
        _trackPoints.value = emptyList()
        lastLocation = null

        startTimer()
    }
    fun stopSession() {
        _isTracking.value = false
        timerJob?.cancel()
    }
    suspend fun updateSession(name: String, description: String?) {
        val sessionId = _currentSessionId.value ?: return // Do nothing if there's no active session

        // Tell the repository to update the session with this ID
        repository.updateSession(sessionId, name, description)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && _isTracking.value) {
                delay(1000)
                _elapsed.value += 1
            }
        }
    }

    suspend fun addCheckpoint() {
        val loc = _trackPoints.value.lastOrNull() ?: return
        val sessionId = repository.getActiveSessionId() ?: return

        val checkpoint = loc.toGpsLocationEntity(
            sessionId = sessionId,
            typeId = C.LOCATION_TYPE_CP
        )

        // update in-memory state
        _checkpoints.value = _checkpoints.value + checkpoint

        // persist
        repository.addLocation(checkpoint)
    }


    fun addWaypoint() {
        _waypoint.value = _trackPoints.value.lastOrNull()
    }

}
