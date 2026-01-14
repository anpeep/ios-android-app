package com.example.gpssportmap.ui.main

import android.location.Location
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpssportmap.data.repository.SessionRepository
import com.example.gpssportmap.data.repository.SettingsRepository
import com.example.gpssportmap.domain.SessionState
import com.example.gpssportmap.domain.SessionTracker
import com.example.gpssportmap.ui.compass.CompassSensor
import com.example.gpssportmap.utils.Utils
import com.example.gpssportmap.utils.Utils.toGpsSettings
import com.example.gpssportmap.utils.Utils.toSessionColorSettings
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    val tracker: SessionTracker,
    val savedStateHandle: SavedStateHandle,
    val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val compassSensor: CompassSensor
) : ViewModel() {
    val checkpointsLatLng: StateFlow<List<LatLng>> = tracker.checkpointsLatLng
    val waypointsLatLng: StateFlow<List<LatLng>> = tracker.waypointsLatLng
    val sessionState: StateFlow<SessionState> = tracker.sessionState
    val elapsedSec: StateFlow<Double> = tracker.elapsedSec
    private val sessionIdFlow: StateFlow<String?> = savedStateHandle.getStateFlow("sessionId", null)
    val wpPathDist: StateFlow<Double> = tracker.distanceAlongPathFromWaypoint
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )
    private val _initialCameraLatLng = MutableStateFlow<LatLng?>(null)
    private val _followUser = MutableStateFlow(false)
    private val _trackPoints = MutableStateFlow<List<Utils.TrackPoint>>(emptyList())
    val trackPoints: StateFlow<List<Utils.TrackPoint>> = _trackPoints

    init {
        viewModelScope.launch {
            val lastLatLng = withContext(Dispatchers.IO) {
                sessionRepository.getLastKnownLatLng()
            }
            _initialCameraLatLng.value = lastLatLng

        }

        viewModelScope.launch {
            tracker.trackPoints.collect { points ->
                _trackPoints.value = points
            }
        }
    }


    fun startCompass() {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                compassSensor.start()
            }
        }
    }

    fun stopCompass() {
        compassSensor.stop()
    }


    val settings: StateFlow<Utils.SessionColorSettings> =
        settingsRepository.settingsFlow
            .map { it.toSessionColorSettings() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = Utils.SessionColorSettings()
            )
    val totalDistanceMeters: StateFlow<Double> = tracker.totalDistanceMeters
    val distanceFromWaypoint: StateFlow<Double> = tracker.distanceFromWaypoint
    val distanceFromCheckpoint: StateFlow<Double> = tracker.distanceFromCheck
    val distanceFromCheckpointDirect: StateFlow<Double> = tracker.distanceAlongPathFromCheckpoint
    val currentLocation: StateFlow<Location?> = tracker.currentLocation
    val previewLocation = tracker.previewLocation
    val segmentPaces: StateFlow<List<Double>> =
        trackPoints
            .map { Utils.computeSegmentPaces(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val azimuth: StateFlow<Float> = compassSensor.azimuth
    val trackLatLng: StateFlow<List<LatLng>> =
        trackPoints.map { list -> list.map { it.latLng } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())


    val paceFromWaypoint: StateFlow<Double> = combine(
        tracker.distanceFromWaypoint,
        tracker.elapsedSec,
        tracker.lastWaypointTimeSec
    ) { dist, now, start ->
        val km = (dist / 1000).coerceAtLeast(0.001)
        val mins = (now - start) / 60.0
        mins / km
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val paceFromStart: StateFlow<Double> =
        combine(tracker.totalDistanceMeters, tracker.elapsedSec) { dist, elapsedSeconds ->
            val km = (dist / 1000).coerceAtLeast(0.001)
            val mins = elapsedSeconds / 60.0
            mins / km
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val paceFromCheckpoint: StateFlow<Double> = combine(
        tracker.distanceFromCheck,
        tracker.elapsedSec,
        tracker.lastCheckpointTimeSec
    ) { dist, now, start ->
        val km = (dist / 1000).coerceAtLeast(0.001)
        val mins = (now - start) / 60.0
        mins / km
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    fun updateSettings(newSettings: Utils.SessionColorSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(newSettings.toGpsSettings())
        }
    }

    fun addWaypoint() {
        viewModelScope.launch {
            val currentLocation = tracker.currentLocation.value ?: return@launch
            tracker.addWaypoint(currentLocation)
        }
    }

    fun addCheckpoint() {
        viewModelScope.launch {
            val currentLocation = tracker.currentLocation.value ?: return@launch
            tracker.addCheckpoint(currentLocation)
        }
    }


    suspend fun startDefaultSession() {
        sessionRepository.initializeIfNeeded()
        startSession()
    }

    fun startSession() {
        tracker.resetSession()
        _followUser.value = true
        viewModelScope.launch {
            runCatching {
                val newSessionId = tracker.startTrackingSession()
                savedStateHandle["sessionId"] = newSessionId
            }.onFailure {
                Log.e("MainViewModel", "Failed to start session", it)
            }
        }
    }


    fun finishAndResetSession(name: String, description: String) {
        viewModelScope.launch {
            val currentSessionId = sessionIdFlow.value
            if (currentSessionId == null) {
                Log.e("MainViewModel", "No active sessionId on finish")
                return@launch
            }

            val duration = tracker.elapsedSec.value
            val distance = tracker.totalDistanceMeters.value
            val avgSpeed = if (duration > 0) distance / duration else 0.0

            sessionRepository.finishSession(
                sessionId = currentSessionId,
                duration = duration,
                distance = distance,
                avgSpeed = avgSpeed
            )

            sessionRepository.updateSession(currentSessionId, name, description)
            tracker.resetSession()
            savedStateHandle["sessionId"] = null
        }
    }

    fun pauseSession() {
        tracker.pauseSession()
    }

    fun resumeSession() {
        tracker.resumeSession()
    }

    override fun onCleared() {
        compassSensor.stop()
        super.onCleared()
    }
}
