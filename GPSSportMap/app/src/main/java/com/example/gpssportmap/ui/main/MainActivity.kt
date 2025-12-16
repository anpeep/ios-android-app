package com.example.gpssportmap.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gpssportmap.data.db.GpsLocationEntity
import com.example.gpssportmap.data.db.GpsSessionEntity
import com.example.gpssportmap.service.LocationService
import com.example.gpssportmap.ui.compass.MapOrientationControls
import com.example.gpssportmap.ui.compass.MapOrientationMode
import com.example.gpssportmap.utils.C
import com.example.gpssportmap.utils.hasBackgroundPermission
import com.example.gpssportmap.utils.hasForegroundPermissions
import com.example.gpssportmap.utils.Utils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this) {
            moveTaskToBack(true)
        }
        setContent {
            AppRoot(
                onStartLocationService = { startTrackingLocationService() },
                onStopLocationService = { stopLocationService() },
                onPreviewLocationService = { startPreviewLocationService() }
            )
        }
    }


    override fun onDestroy() {
        super.onDestroy()
    }

    private fun startPreviewLocationService() {
        val intent = Intent(this, LocationService::class.java).apply {
            action = "ACTION_PREVIEW"
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun startTrackingLocationService() {
        val intent = Intent(this, LocationService::class.java).apply {
            action = "ACTION_START"
        }
        ContextCompat.startForegroundService(this, intent)
    }
    private fun stopLocationService() {
        val intent = Intent(this, LocationService::class.java).apply {
            action = "ACTION_STOP"
        }
        startService(intent)
    }
}

@Composable
fun AppRoot(
    onStartLocationService: () -> Unit,
    onStopLocationService: () -> Unit,
    onPreviewLocationService: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "auth") {
        composable("auth") {
            val authVm: AuthViewModel = hiltViewModel()
            AuthScreen(viewModel = authVm) {
                navController.navigate("main") {
                    popUpTo("auth") { inclusive = true }
                }
            }
        }


        composable("main") {
            PermissionController(
                onPermissionsGranted = {
                    MainContent(
                        onStartLocationService = onStartLocationService,
                        onStopLocationService = onStopLocationService,
                        navController = navController,
                        onPreviewLocationService = onPreviewLocationService
                    )
                }
            )
        }
        composable("old_sessions") {
            val mainVm: MainViewModel = hiltViewModel()
            OldSessionsScreen(mainVm = mainVm)
        }
    }
}

@Composable
fun MainContent(
    onStartLocationService: () -> Unit,
    onStopLocationService: () -> Unit,
    navController: NavController,
    onPreviewLocationService: () -> Unit
) {
    val mainVm: MainViewModel = hiltViewModel()
    LaunchedEffect(Unit) {
        onPreviewLocationService()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    if (showSaveDialog) {
        SaveSessionDialog(
            onDismiss = {
                showSaveDialog = false
                onStopLocationService()
            },
            onConfirm = { name, description ->
                showSaveDialog = false
                mainVm.updateSession(name, description)
                onStopLocationService()
            }
        )
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mainVm.startCompass()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    MainScreen(
        vm = mainVm,
        onStartLocationService = onStartLocationService,
        onStopLocationService = { showSaveDialog = true },
        onOpenHistory = { navController.navigate("old_sessions") }
    )

}
@Composable
fun SaveSessionDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Save Session") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Session Name") },
                    singleLine = true,
                    placeholder = { Text("e.g., Morning Run") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank()
            ) {
                Text("Save & Reset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Discard & Reset")
            }
        }
    )
}
@Composable
fun OldSessionsScreen(mainVm: MainViewModel) {
    val savedSessions by mainVm.savedSessions.collectAsState()

    LaunchedEffect(Unit) {
        mainVm.loadOldSessions()
    }
    if (savedSessions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No saved sessions found.")
        }
    } else {
        LazyColumn {
                items(savedSessions) { session ->
                    SessionListItem(
                        session = session,
                        onDeleteClick = {
                            session.id.let { idString ->
                                mainVm.deleteSession(UUID.fromString(session.name))
                            }
                        },
                        onItemClick = {
                            // Handle navigation to session details if needed
                        }
                    )
                }
            }
        }
    }


@Composable
fun SessionListItem(
    session: GpsSessionEntity,
    onDeleteClick: () -> Unit,
    onItemClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(session.name)
        }
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Session"
            )
        }
    }
}


@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun LiveMapWithCompass(
    points: List<LatLng>,
    waypoint: LatLng?,
    checkpoints: List<LatLng>,
    latitude: Double,
    longitude: Double,
    azimuth: Float
) {
    val isLandscape = isLandscape()
    var orientationMode by remember { mutableStateOf(MapOrientationMode.COMPASS) }
    val latLngs = points
    val currentLatLng = LatLng(latitude, longitude)
    Box(Modifier.fillMaxSize()) {
        val cameraPositionState = rememberCameraPositionState()

        LaunchedEffect(currentLatLng) {
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f)
            )
        }

        LaunchedEffect(cameraPositionState.isMoving) {
            if (cameraPositionState.isMoving && orientationMode != MapOrientationMode.USER_CHOOSE) {
                if(orientationMode == MapOrientationMode.CENTER || orientationMode == MapOrientationMode.COMPASS) {
                    orientationMode = MapOrientationMode.USER_CHOOSE
                }
            }
        }
        LaunchedEffect(currentLatLng, orientationMode, azimuth) {
            if (orientationMode == MapOrientationMode.CENTER) {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLng(currentLatLng),
                    durationMs = 500
                )
            }
            val bearingTarget = when (orientationMode) {
                MapOrientationMode.COMPASS -> -azimuth
                MapOrientationMode.NORTH -> 0f
                else -> null
            }

            if (bearingTarget != null && cameraPositionState.position.bearing != bearingTarget) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.builder(cameraPositionState.position)
                            .bearing(bearingTarget)
                            .build()
                    ),
                    durationMs = 200
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                cameraPositionState = cameraPositionState,
                modifier = Modifier.fillMaxSize(),

            ) {
                if (latLngs.isNotEmpty()) {
                    Polyline(points = latLngs, width = 8f)
                    Marker(state = MarkerState(latLngs.first()), title = "Start")
                }
                if (latitude != 0.0 && longitude != 0.0) {
                    Marker(
                        state = MarkerState(currentLatLng),
                        title = "You",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }
                waypoint?.let  { wp ->
                    Marker(
                        state = MarkerState(wp),
                        title = "WP",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )
                }
                checkpoints.forEachIndexed { i, cp ->
                    Marker(
                        state = MarkerState(cp), // Use cp, not LatLng(latitude, longitude)
                        title = "CP ${i + 1}",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                    )
                }
            }
            MapOrientationControls(
                modifier = Modifier
                    .align(
                        if (isLandscape) Alignment.CenterEnd else Alignment.TopEnd
                    )
                    .padding(16.dp),
                initialMode = orientationMode,
                onModeChange = { newMode ->
                    orientationMode = newMode
                }
            )
        }
    }
}
@Composable
fun isLandscape(): Boolean {
    val config = LocalConfiguration.current
    return config.orientation == Configuration.ORIENTATION_LANDSCAPE
}
@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
@Composable
fun MainScreen(
    vm: MainViewModel,
    onStartLocationService: () -> Unit,
    onStopLocationService: () -> Unit,
    onReset: () -> Unit = {},
    onOpenHistory: () -> Unit
)
{
    val isLandscape = isLandscape()
    val azimuth by vm.azimuth.collectAsState()
    val location by vm.currentLocation.collectAsState()

    val latitude = location?.latitude
    val longitude = location?.longitude

    val isTracking by vm.isTracking.collectAsState()

    val elapsedSec by vm.elapsedSec.collectAsState()
    val totalDist by vm.totalDistanceMeters.collectAsState()
    val cpDist by vm.distanceFromCheckpoint.collectAsState()
    val wpDist by vm.distanceFromWaypoint.collectAsState()
    val pace by vm.currentPaceMinPerKm.collectAsState()

    val safeLatLng = remember(latitude, longitude) {
        if (
            latitude != null &&
            longitude != null &&
            latitude in -90.0..90.0 &&
            longitude in -180.0..180.0
        ) {
            LatLng(latitude, longitude)
        } else null
    }


    if (isLandscape) {
        Row(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(140.dp) // Narrower column for just buttons
                    .padding(vertical = 16.dp, horizontal = 8.dp),
                verticalArrangement = Arrangement.SpaceAround, // Evenly space the buttons
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (!isTracking) {
                            vm.startSession(
                                name = "Session",
                                description = null,
                                sessionTypeId = "DEFAULT"
                            )
                            onStartLocationService()
                        } else {
                            vm.stopSession()
                            onStopLocationService()
                        }
                    }
                ) {
                    Text(if (isTracking) "STOP" else "START")
                }
                Button(onClick = { vm.addCheckpointUi() }, modifier = Modifier.fillMaxWidth()) {
                    Text("CP +")
                }
                Button(onClick = { vm.addWaypointUi()
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("WP +")
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(220.dp) // Wider column for text
                    .padding(vertical = 16.dp, horizontal = 8.dp),
                verticalArrangement = Arrangement.SpaceAround,
                horizontalAlignment = Alignment.Start
            ) {
                // Session Stats
                StatItem(label = "Duration", value = Utils.formatTime(elapsedSec))
                StatItem(label = "Distance", value = "${totalDist.toInt()} m")
                StatItem(label = "Avg. Pace", value = Utils.formatPace(pace))

                // Checkpoint Stats
                StatItem(label = "CP Direct", value = "${cpDist.toInt()} m")
                StatItem(label = "CP Pace", value = Utils.formatPace(vm.paceFromCheckpoint.collectAsState().value))

                // Waypoint Stats
                StatItem(label = "WP Travelled", value = "${wpDist.toInt()} m")
                StatItem(label = "WP Pace", value = Utils.formatPace(vm.paceFromWaypoint.collectAsState().value))
            }

            // 🗺️ 2. CENTER MAP (takes up the remaining space between the side panels)
            Box(Modifier.weight(1f)) {
                if (safeLatLng != null) {
                    LiveMapWithCompass(
                        points = vm.trackPoints.collectAsState().value.map {
                            LatLng(it.latitude, it.longitude)
                        },
                        waypoint = vm.waypoint.collectAsState().value?.let {
                            LatLng(it.latitude, it.longitude)
                        },
                        checkpoints = vm.checkpoints.collectAsState().value.map {
                            LatLng(it.latitude, it.longitude)
                        },
                        latitude = safeLatLng.latitude,
                        longitude = safeLatLng.longitude,
                        azimuth = azimuth
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    TextButton(onClick = onReset) { Text("Reset") }
                    TextButton(onClick = onOpenHistory) { Text("Options") }
                }
            }
        }

    } else {
        Column(Modifier.fillMaxSize()) {

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onReset) { Text("Reset") }
                TextButton(onClick = onOpenHistory) { Text("Options") }
            }

            Box(Modifier.weight(1f)) {
Log.d("MainScreen", "Latitude: $latitude, points: $latitude, Longitude: $longitude, Azimuth: $azimuth")
                    if (latitude != null && longitude != null) {


                        LiveMapWithCompass(
                            points = vm.trackPoints.collectAsState().value.map {
                                LatLng(it.latitude, it.longitude)
                            },
                            waypoint = vm.waypoint.collectAsState().value?.let {
                                LatLng(it.latitude, it.longitude)
                            },
                            checkpoints = vm.checkpoints.collectAsState().value.map {
                                LatLng(it.latitude, it.longitude)
                            },
                            latitude = latitude,
                            longitude = longitude,
                            azimuth = azimuth
                        )

                    } else {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }


            }


            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            if (!isTracking) {
                                vm.startSession(
                                    name = "Session",
                                    description = null,
                                    sessionTypeId = "DEFAULT"
                                )
                                onStartLocationService()
                            } else {
                                vm.stopSession()
                                onStopLocationService()
                            }
                        }
                    ) {
                        Text(if (isTracking) "STOP" else "START")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${totalDist.toInt()} m")
                        Button(onClick = { vm.addCheckpointUi() }) { Text("CP +") }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${wpDist} m")
                        Button(onClick = { vm.addWaypointUi() }) { Text("WP +") }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Utils.formatTime(elapsedSec))
                        Text("Pace: ${Utils.formatPace(pace)}")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$cpDist m")
                        Text("Pace: ${Utils.formatPace(vm.paceFromCheckpoint.collectAsState().value)}")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Pace: ${Utils.formatPace(vm.paceFromWaypoint.collectAsState().value)}")
                    }
                }
            }
        }
    }
}



        @Composable
fun PermissionController(
    onPermissionsGranted: @Composable () -> Unit
) {
    val context = LocalContext.current // Get the context here

    var permissionsState by remember {
        mutableStateOf(
            mapOf(
                "foreground" to context.hasForegroundPermissions(),
                "background" to context.hasBackgroundPermission()
            )
        )
    }
    val foregroundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsResult ->
            permissionsState = permissionsState.toMutableMap().apply {
                this["foreground"] = permissionsResult.values.all { it }
            }
        }
    )
    val backgroundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            permissionsState = permissionsState.toMutableMap().apply {
                this["background"] = isGranted
            }
        }
    )
    when {
        permissionsState["foreground"] == true && permissionsState["background"] == true -> {
            onPermissionsGranted()
        }
        permissionsState["foreground"] == true -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Allow Location Access All the Time", fontSize = 20.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Text("This app tracks your activity even when closed. Please select 'Allow all the time' to enable this.", textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }) {
                    Text("Continue")
                }
            }
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Permissions Required", fontSize = 20.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Text("This app needs Location and Notification permissions to function.", textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    foregroundLauncher.launch(permissions)
                }) {
                    Text("Grant Permissions")
                }
            }
        }
    }
}

