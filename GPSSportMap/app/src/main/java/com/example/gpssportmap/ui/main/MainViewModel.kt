
package com.example.gpssportmap.ui.main

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpssportmap.data.db.GpsLocationEntity
import com.example.gpssportmap.data.db.GpsSessionEntity
import com.example.gpssportmap.data.db.GpsSessionTypeEntity
import com.example.gpssportmap.data.repository.SessionRepository
import com.example.gpssportmap.domain.model.SessionTracker
import com.example.gpssportmap.utils.CompassSensor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val tracker: SessionTracker,
    private val sessionRepository: SessionRepository,
    private val compassSensor: CompassSensor
) : ViewModel() {

    init {
        compassSensor.start()
    }
    fun startCompass() = compassSensor.start()
    val isTracking: StateFlow<Boolean> = tracker.isTracking
    val trackPoints: StateFlow<List<Location>> = tracker.trackPoints
    val elapsedSec: StateFlow<Long> = tracker.elapsed
    val azimuth: StateFlow<Float> = compassSensor.azimuth
    val waypoint: StateFlow<Location?> = tracker.waypoint
    val checkpoints: StateFlow<List<GpsLocationEntity>> = tracker.checkpoints



    val currentLocation: StateFlow<Location?> =
        trackPoints
            .map { it.lastOrNull() }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                null
            )



    val totalDistanceMeters: StateFlow<Double> =
        tracker.distance
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                0.0
            )



    val distanceFromCheckpoint: StateFlow<Double> =
        combine(checkpoints, currentLocation) { cps, cur ->
            val cp = cps.lastOrNull() ?: return@combine 0.0
            val loc = cur ?: return@combine 0.0

            val cpLoc = Location("").apply {
                latitude = cp.latitude
                longitude = cp.longitude
            }
            cpLoc.distanceTo(loc).toDouble()
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            0.0
        )

    val distanceFromWaypoint: StateFlow<Double> =
        combine(waypoint, currentLocation) { wp, cur ->
            if (wp == null || cur == null) 0.0
            else wp.distanceTo(cur).toDouble()
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            0.0
        )


    val currentPaceMinPerKm: StateFlow<Double> =
        combine(totalDistanceMeters, elapsedSec) { dist, time ->
            val km = (dist / 1000.0).coerceAtLeast(0.001)
            val mins = time / 60.0
            mins / km
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            0.0
        )

    val paceFromWaypoint: StateFlow<Double> =
        combine(distanceFromWaypoint, elapsedSec) { dist, time ->
            val km = (dist / 1000.0).coerceAtLeast(0.001)
            val mins = time / 60.0
            mins / km
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            0.0
        )

    val paceFromCheckpoint: StateFlow<Double> =
        combine(checkpoints, elapsedSec, distanceFromCheckpoint) { cps, time, dist ->
            val last = cps.lastOrNull() ?: return@combine 0.0
            val lastTime = Instant.parse(last.recordedAt).epochSecond
            val mins = ((time - lastTime).coerceAtLeast(0)) / 60.0
            val km = (dist / 1000.0).coerceAtLeast(0.001)
            mins / km
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            0.0
        )

    fun startSession(name: String, description: String?, sessionTypeId: String) {
        viewModelScope.launch {
            tracker.startSession(name, description, sessionTypeId)
        }
    }

    fun stopSession() {
        tracker.stopSession()
    }

    fun addWaypointUi() {
        tracker.addWaypoint()
    }

    fun addCheckpointUi() {
        viewModelScope.launch {
            tracker.addCheckpoint()
        }
    }

    fun updateSession(name: String, description: String?) {
        viewModelScope.launch {
            tracker.updateSession(name, description)
        }
    }

    private val _savedSessions = MutableStateFlow<List<GpsSessionEntity>>(emptyList())
    val savedSessions = _savedSessions.asStateFlow()

    fun loadOldSessions() {
        viewModelScope.launch {
            sessionRepository.getAllSessions()
                .collect { _savedSessions.value = it }
        }
    }

    fun deleteSession(sessionId: UUID) {
        viewModelScope.launch {
            sessionRepository.deleteSession(sessionId.toString())
            loadOldSessions()
        }
    }

    override fun onCleared() {
        compassSensor.stop()
        super.onCleared()
    }
}
