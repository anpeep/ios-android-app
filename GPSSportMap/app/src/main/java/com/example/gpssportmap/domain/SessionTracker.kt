package com.example.gpssportmap.domain

import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.example.gpssportmap.data.mappers.toGpsLocationCreateDto
import com.example.gpssportmap.data.repository.SessionRepository
import com.example.gpssportmap.utils.C
import com.example.gpssportmap.utils.Utils
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionTracker @Inject constructor(
    private val repository: SessionRepository,
    @ApplicationContext private val context: Context,

    ) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _distanceAlongPathFromCheckpoint = MutableStateFlow(0.0)
    val distanceAlongPathFromCheckpoint: StateFlow<Double> = _distanceAlongPathFromCheckpoint

    private val _trackPoints = MutableStateFlow<List<Utils.TrackPoint>>(emptyList())
    val trackPoints: StateFlow<List<Utils.TrackPoint>> = _trackPoints.asStateFlow()
    private val _trackLocations = MutableStateFlow<List<Location>>(emptyList())
    private val _lastWaypoint = MutableStateFlow<Location?>(null)
    private val _checkpointsLatLng = MutableStateFlow<List<LatLng>>(emptyList())
    val checkpointsLatLng: StateFlow<List<LatLng>> = _checkpointsLatLng
    private var timerJob: Job? = null
    private val _sessionState = MutableStateFlow(SessionState.IDLE)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()
    private var lastSavedLocation: Location? = null
    private var _elapsedSec = MutableStateFlow(0.0)
    var elapsedSec: StateFlow<Double> = _elapsedSec.asStateFlow()
    private var _totalDistanceMeters = MutableStateFlow(0.0)
    var totalDistanceMeters: StateFlow<Double> = _totalDistanceMeters.asStateFlow()
    private val _distanceFromLastCheckpoint = MutableStateFlow(0.0)
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()
    val lastCheckpointTimeSec = MutableStateFlow(0L)
    val lastWaypointTimeSec = MutableStateFlow(0L)
    private val _syncIntervalSec = MutableStateFlow(10)
    val syncIntervalSec = _syncIntervalSec.asStateFlow()
    private var syncJob: Job? = null

    private val _distanceFromLastWaypoint = MutableStateFlow(0.0)
    val distanceFromWaypoint: StateFlow<Double> = _distanceFromLastWaypoint.asStateFlow()
    val distanceFromCheck: StateFlow<Double> = _distanceFromLastCheckpoint.asStateFlow()
    private val _waypointsLatLng = MutableStateFlow<List<LatLng>>(emptyList())
    val waypointsLatLng: StateFlow<List<LatLng>> get() = _waypointsLatLng
    private val _previewLocation = MutableStateFlow<Location?>(null)
    val previewLocation: StateFlow<Location?> = _previewLocation

    private val _distanceAlongPathFromWaypoint = MutableStateFlow(0.0)  // Accumulated path distance
    val distanceAlongPathFromWaypoint: StateFlow<Double> = _distanceAlongPathFromWaypoint


    suspend fun addCheckpoint(loc: Location) {
        val sessionId = _activeSessionId.value ?: return
        if (_sessionState.value != SessionState.RUNNING) return

        val dto = loc.toGpsLocationCreateDto(
            locationTypeId = C.LOCATION_TYPE_CP
        )

        try {
            repository.addLocation(sessionId, dto, C.LOCATION_TYPE_CP)
        } catch (e: HttpException) {
            Log.e("LocationUpload", "Failed: ${e.code()}")
        }
        _checkpointsLatLng.update {
            it + LatLng(loc.latitude, loc.longitude)
        }
        lastCheckpointTimeSec.value = _elapsedSec.value.toLong()
        _distanceFromLastCheckpoint.value = 0.0
        _distanceAlongPathFromCheckpoint.value = 0.0
        notify(C.LOCATION_TYPE_CP)
    }

    suspend fun addWaypoint(loc: Location) {
        if (_sessionState.value != SessionState.RUNNING) return
        val sessionId = currentSessionId.value ?: return

        val dto = loc.toGpsLocationCreateDto(
            locationTypeId = C.LOCATION_TYPE_WP
        )

        repository.addLocation(sessionId, dto, C.LOCATION_TYPE_WP)
        _lastWaypoint.value = loc
        _waypointsLatLng.update { it + LatLng(loc.latitude, loc.longitude) }

        lastWaypointTimeSec.value = _elapsedSec.value.toLong()
        _distanceFromLastWaypoint.value = 0.0
        _distanceAlongPathFromWaypoint.value = 0.0

        notify(C.LOCATION_TYPE_WP)
    }

    private var lastSentLocation: Location? = null


    suspend fun onLocationUpdate(location: Location) {
        val lastValidLocation = _trackLocations.value.lastOrNull()
        if (lastValidLocation != null) {
            val distanceToLastPoint = location.distanceTo(lastValidLocation)
            val dt = (location.time - lastValidLocation.time).coerceAtLeast(1000)
            val speed = distanceToLastPoint / (dt / 1000f)

            if (speed > 15f) { // 15 m/s = 54 km/h
                Log.w("GpsFilter", "Ignoring unrealistic speed: $speed m/s")
                return
            }

        }
        _previewLocation.value = location
        _currentLocation.value = location
        _trackPoints.update {
            it + Utils.TrackPoint(
                latLng = LatLng(location.latitude, location.longitude),
                timeMillis = location.time
            )
        }
        val sessionId = _activeSessionId.value ?: return
        if (_sessionState.value != SessionState.RUNNING) return
        _trackLocations.update { it + location }


        val updatedLocations = _trackLocations.value


        if (updatedLocations.size > 1) {
            val lastLocation = updatedLocations[updatedLocations.size - 1]
            val secondToLastLocation = updatedLocations[updatedLocations.size - 2]
            val distanceSegment = lastLocation.distanceTo(secondToLastLocation).toDouble()
            _totalDistanceMeters.update { currentTotal -> currentTotal + distanceSegment }
        }
        val dto = location.toGpsLocationCreateDto(
            locationTypeId = C.LOCATION_TYPE_LOC
        )

        val lastWpLocation = _lastWaypoint.value
        if (lastWpLocation != null) {

            val lastWaypointLocation = Location("").apply {
                latitude = lastWpLocation.latitude
                longitude = lastWpLocation.longitude
            }


            _distanceFromLastWaypoint.value = location.distanceTo(lastWaypointLocation).toDouble()


            val lastWaypointIndex = _trackLocations.value.indexOfLast {
                it.latitude == lastWpLocation.latitude &&
                        it.longitude == lastWpLocation.longitude
            }

            val pathDistance = _trackLocations.value
                .subList(lastWaypointIndex.coerceAtLeast(0), _trackLocations.value.size - 1)
                .zipWithNext { a, b -> a.distanceTo(b).toDouble() }
                .sum()

            _distanceAlongPathFromWaypoint.value = pathDistance
        }

        val lastCheckpointLatLng = _checkpointsLatLng.value.lastOrNull()
        if (lastCheckpointLatLng != null) {
            val lastCheckpointLocation = Location("").apply {
                latitude = lastCheckpointLatLng.latitude
                longitude = lastCheckpointLatLng.longitude
            }
            _distanceFromLastCheckpoint.value =
                location.distanceTo(lastCheckpointLocation).toDouble()

            val lastCheckpointIndex = _trackLocations.value.indexOfLast {
                it.latitude == lastCheckpointLatLng.latitude &&
                        it.longitude == lastCheckpointLatLng.longitude
            }

            val pathDistance = _trackLocations.value
                .subList(lastCheckpointIndex.coerceAtLeast(0), _trackLocations.value.size - 1)
                .zipWithNext { a, b -> a.distanceTo(b).toDouble() }
                .sum()

            _distanceAlongPathFromCheckpoint.value = pathDistance

        }

        repository.addLocation(sessionId, dto, C.LOCATION_TYPE_LOC)
    }

    fun updateSyncInterval(seconds: Int) {
        syncJob?.cancel()
        _syncIntervalSec.value = seconds
    }


    private val _activeSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    suspend fun startTrackingSession(): String {
        val sessionId = repository.startSession()
        _activeSessionId.value = sessionId
        _sessionState.value = SessionState.RUNNING
        startTimer()
        startSyncing(sessionId)
        return sessionId
    }


    private fun startTimer() {
        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(1000)
                _elapsedSec.value += 1
            }
        }
    }

    fun startSyncing(sessionId: String) {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            syncIntervalSec.collectLatest { interval ->
                while (isActive) {
                    withContext(Dispatchers.IO) {
                        repository.syncSessionLocations(sessionId)
                    }
                    delay(interval * 1000L)
                }
            }
        }
    }

    fun pauseSession() {
        _sessionState.value = SessionState.PAUSED
        timerJob?.cancel()
    }

    fun resumeSession() {
        _sessionState.value = SessionState.RUNNING
        startTimer()
    }

    private fun notify(action: String) {
        context.sendBroadcast(Intent(action))
    }


    fun resetSession() {
        val duration = elapsedSec.value
        val distance = totalDistanceMeters.value
        val avgPace =
            if (distance > 0) (duration / 60.0) / (distance / 1000.0) else 0.0

        serviceScope.launch {
            currentSessionId.value?.let { id ->
                repository.finishSession(id, duration, distance, avgPace)
            }
        }
        _sessionState.value = SessionState.IDLE
        timerJob?.cancel()
        _elapsedSec.value = 0.0
        _totalDistanceMeters.value = 0.0
        _trackLocations.value = emptyList()
        _trackPoints.value = emptyList()
        _checkpointsLatLng.value = emptyList()
        _waypointsLatLng.value = emptyList()
        _activeSessionId.value = null
        _distanceFromLastWaypoint.value = 0.0
        _distanceFromLastCheckpoint.value = 0.0
        lastWaypointTimeSec.value = 0L
        lastCheckpointTimeSec.value = 0L

        lastSavedLocation = null
        _lastWaypoint.value = null
        lastSentLocation = null
    }


}

enum class SessionState {
    IDLE, RUNNING, PAUSED
}
