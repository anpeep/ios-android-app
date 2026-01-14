package com.example.gpssportmap.ui.main

import android.location.Location
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpssportmap.data.db.entities.GpsLocationsEntity
import com.example.gpssportmap.data.db.entities.GpsSessionsEntity
import com.example.gpssportmap.data.repository.SessionRepository
import com.example.gpssportmap.data.repository.SettingsRepository
import com.example.gpssportmap.utils.C
import com.example.gpssportmap.utils.GpxExporter
import com.example.gpssportmap.utils.Utils
import com.example.gpssportmap.utils.Utils.computeSegmentPaces
import com.example.gpssportmap.utils.Utils.toSessionColorSettings
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val repo: SessionRepository,
    savedStateHandle: SavedStateHandle,
    private val gpxExporter: GpxExporter,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val sessionId: String = savedStateHandle.get<String>("sessionId")!!

   
    private val _azimuth = MutableStateFlow(0f)
    val azimuth: StateFlow<Float> = _azimuth

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionDetails: StateFlow<GpsSessionsEntity?> = flowOf(sessionId)
        .flatMapLatest { id -> repo.getSessionByIdFlow(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val trackPoints: StateFlow<List<GpsLocationsEntity>> =
        repo.getLocationsForSession(sessionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val trackLatLng: StateFlow<List<LatLng>> = trackPoints.map { points ->
        points.map { LatLng(it.latitude, it.longitude) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings: StateFlow<Utils.SessionColorSettings> = settingsRepository.settingsFlow
        .map { it.toSessionColorSettings() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Utils.SessionColorSettings()
        )

    fun Utils.SessionColorSettings.toGpsSettings(): Utils.SessionColorSettings {
        return Utils.SessionColorSettings(paceMin = this.paceMin, paceMax = this.paceMax)
    }

    init {
        viewModelScope.launch {
            repo.syncSessionLocations(sessionId)

        }
    }

    fun updateSettings(settings: Utils.SessionColorSettings) {
        viewModelScope.launch {
           
            settingsRepository.updateSettings(settings.toGpsSettings())
        }
    }

    val pointPaces: StateFlow<List<Double>> = trackPoints.map { points ->
        val trackPointList = points.map { entity ->
            Utils.TrackPoint(
                latLng = LatLng(entity.latitude, entity.longitude),
                timeMillis = Instant.parse(entity.recordedAt).toEpochMilli()
            )
        }
        computeSegmentPaces(trackPointList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val checkpointsLatLng: StateFlow<List<LatLng>> = flowOf(sessionId).flatMapLatest { id ->
        repo.getSessionCheckpoints(id).map { list ->
            list.map { LatLng(it.latitude, it.longitude) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val waypointsLatLng: StateFlow<List<LatLng>> = flowOf(sessionId).flatMapLatest { id ->
        repo.getSessionWaypoints(id).map { list ->
            list.map { LatLng(it.latitude, it.longitude) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val duration: StateFlow<Double> = trackPoints.map { points ->
        if (points.size < 2) 0.0 else {
            val first = Instant.parse(points.first().recordedAt).toEpochMilli()
            val last = Instant.parse(points.last().recordedAt).toEpochMilli()
            (last - first) / 1000.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val distance: StateFlow<Double> = trackPoints.map { points ->
        if (points.size < 2) 0.0 else {
            points.map { LatLng(it.latitude, it.longitude) }
                .zipWithNext { a, b ->
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        a.latitude,
                        a.longitude,
                        b.latitude,
                        b.longitude,
                        results
                    )
                    results[0].toDouble()
                }.sum()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val pace: StateFlow<Double> = combine(duration, distance) { dur, dist ->
        if (dist <= 0 || dur <= 0) 0.0 else (dur / 60.0) / (dist / 1000.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    private val _exportEvents = MutableSharedFlow<String>()
    val exportEvents = _exportEvents.asSharedFlow()
    fun onExportGpxClicked() {
        viewModelScope.launch {
            val allLocations = repo.getLocationsForSession(sessionId).first()
            val trackPoints = allLocations.filter { it.gpsLocationTypeId == C.LOCATION_TYPE_LOC }
            val checkpoints = allLocations.filter { it.gpsLocationTypeId == C.LOCATION_TYPE_CP }
            val waypoints = allLocations.filter { it.gpsLocationTypeId == C.LOCATION_TYPE_WP }
            if (trackPoints.isEmpty()) return@launch
            val gpx = gpxExporter.buildGpx(trackPoints, checkpoints, waypoints)
            _exportEvents.emit(gpx)
        }
    }

}
