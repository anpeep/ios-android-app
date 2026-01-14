package com.example.gpssportmap.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gpssportmap.R
import com.example.gpssportmap.data.db.entities.GpsSessionsEntity
import com.example.gpssportmap.data.network.ApiService
import com.example.gpssportmap.data.repository.SessionRepository
import com.example.gpssportmap.domain.NotificationService
import com.example.gpssportmap.domain.SessionState
import com.example.gpssportmap.ui.auth.AuthScreen
import com.example.gpssportmap.ui.auth.AuthViewModel
import com.example.gpssportmap.ui.auth.hasBackgroundPermission
import com.example.gpssportmap.ui.auth.hasForegroundPermissions
import com.example.gpssportmap.ui.compass.CompassView
import com.example.gpssportmap.ui.compass.MapOrientationControls
import com.example.gpssportmap.ui.compass.MapOrientationMode
import com.example.gpssportmap.utils.C
import com.example.gpssportmap.utils.Utils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var sessionRepository: SessionRepository
    @Inject
    lateinit var api: ApiService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val onStartNotificationService = {
            ContextCompat.startForegroundService(
                this,
                Intent(this, NotificationService::class.java).apply { action = C.ACTION_START })
        }
        setContent {
            AppRoot(
                onStartLocationService = onStartNotificationService,
            )
        }
    }

    override fun onDestroy() {
        stopLocationService()
        super.onDestroy()
    }

    private fun stopLocationService() {
        val intent =
            Intent(this, NotificationService::class.java).apply { action = "ACTION_STOP" }
        ContextCompat.startForegroundService(this, intent)
    }
}

@Composable
fun AppRoot(
    onStartLocationService: () -> Unit,
) {
    val navController = rememberNavController()
    val authVm: AuthViewModel = hiltViewModel()
    val startDestination = if (authVm.isLoggedIn()) "main" else "auth"

    NavHost(navController, startDestination = startDestination) {
        composable("auth") {
            AuthScreen(viewModel = authVm) {
                navController.navigate("main") {
                    popUpTo("auth") {
                        inclusive = true
                    }
                }
            }
        }
        composable("main") {
            PermissionController(onPermissionsGranted = {
                val mainVm: MainViewModel = hiltViewModel()

                MainContent(
                    onStartLocationService = onStartLocationService,
                    navController = navController,
                    vm = mainVm
                )
            })
        }
        composable("old_sessions") {
            val mainVm: MainViewModel = hiltViewModel()
            OldSessionsScreen(repo = mainVm.sessionRepository, navController = navController)
        }
        composable(
            route = "session_detail/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->

            val sessionId = backStackEntry.arguments!!.getString("sessionId")!!

            val viewModel: SessionDetailViewModel = hiltViewModel(
                key = "SessionDetailViewModel_$sessionId"
            )

            SessionDetailScreen(
                sessionVm = viewModel,
                navController = navController
            )
        }
    }
}

@Composable
fun MainContent(
    onStartLocationService: () -> Unit,
    navController: NavController,
    vm: MainViewModel
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.startCompass()
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                vm.stopCompass()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }

    if (showSaveDialog) {
        SaveSessionDialog(
            onDismiss = { showSaveDialog = false },
            onConfirm = { name, description ->
                showSaveDialog = false
                vm.finishAndResetSession(name, description)
            }
        )
    }

    MainScreen(
        vm = vm,
        onStartLocationService = onStartLocationService,
        onReset = { showSaveDialog = true },
        onOpenHistory = { navController.navigate("old_sessions") }
    )
}

private fun shareGpx(context: Context, gpxContent: String) {
    try {
        val file = File(context.cacheDir, "session.gpx")
        file.writeText(gpxContent)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share GPX"))
    } catch (e: Exception) {
        Log.e("ShareGpx", "Failed to share GPX file", e)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionVm: SessionDetailViewModel = hiltViewModel(),
    navController: NavController
) {
    var isCompassVisible by remember { mutableStateOf(true) }

    val pointPaces by sessionVm.pointPaces.collectAsState()
    val settings by sessionVm.settings.collectAsState()
    val context = LocalContext.current
    val checkpoints by sessionVm.checkpointsLatLng.collectAsState()
    val waypoints by sessionVm.waypointsLatLng.collectAsState()
    val points by sessionVm.trackLatLng.collectAsState()
    val cameraPositionState = rememberCameraPositionState()
    val session by sessionVm.sessionDetails.collectAsState()
    val pace by sessionVm.pace.collectAsState()
    val distance by sessionVm.distance.collectAsState()
    val duration by sessionVm.duration.collectAsState()
    var showOptions by remember { mutableStateOf(false) }
    val azimuth = sessionVm.azimuth.collectAsState().value
    if (showOptions) {
        SessionOptionsSheet(onDismiss = { showOptions = false }) {
            Column(
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Track Coloring", style = MaterialTheme.typography.titleMedium)
                SpeedColorSettings(
                    settings = settings,
                    onChange = { newSettings ->
                        sessionVm.updateSettings(newSettings)
                    },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )
            }
        }
    }
    LaunchedEffect(Unit) { sessionVm.exportEvents.collect { gpx -> shareGpx(context, gpx) } }
    LaunchedEffect(points.size) {
        if (points.size > 1) {
            val bounds = LatLngBounds.builder().apply { points.forEach { include(it) } }.build()
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 100), 1000)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session?.name ?: "Session Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }, actions = {
                    IconButton(onClick = { showOptions = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_options),
                            contentDescription = "Options"
                        )
                    }
                    IconButton(
                        enabled = points.isNotEmpty(),
                        onClick = { sessionVm.onExportGpxClicked() }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_share),
                            contentDescription = "Share as GPX"
                        )
                    }
                    if (isCompassVisible) {
                        CompassView(
                            azimuth = azimuth,
                            modifier = Modifier
                                .clickable { isCompassVisible = false },
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Box(Modifier.weight(1f)) {
                LiveMapWithCompass(
                    points = points,
                    checkpoints = checkpoints,
                    waypoints = waypoints,
                    azimuth = azimuth,
                    sessionState = SessionState.IDLE,
                    currentLatLng = points.lastOrNull() ?: return@Column,
                    cameraPositionState = cameraPositionState,
                    settings = settings,
                    segmentPaces = pointPaces
                )
            }
            session?.let { details ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItem(
                        label = "Duration",
                        value = Utils.formatTime(duration)
                    )

                    StatItem(
                        label = "Distance",
                        value = "${distance.toInt()} m"
                    )

                    StatItem(
                        label = "Avg. Pace",
                        value = Utils.formatPace(pace)
                    )
                }
            }
        }
    }
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
                    label = { Text("Description") }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OldSessionsScreen(repo: SessionRepository, navController: NavController) {
    val scope = rememberCoroutineScope()
    val savedSessions by repo.getAllSessions().collectAsState(initial = emptyList())
    var renameSession by remember { mutableStateOf<GpsSessionsEntity?>(null) }
    var newName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Back to record") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        if (savedSessions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No saved sessions found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(savedSessions) { session ->
                    SessionListItem(
                        session = session,
                        onItemClick = {
                            navController.navigate("session_detail/${session.id}")
                        },
                        onDeleteClick = {
                            scope.launch {
                                repo.deleteSession(session.id)
                            }
                        },
                        onRenameClick = {
                            renameSession = session
                        }
                    )
                }
            }
        }
        renameSession?.let { session ->
           
            LaunchedEffect(session) {
                newName = session.name
            }

            RenameSessionDialog(
                currentName = newName,
                onValueChange = { newName = it },
                onDismiss = { renameSession = null },
                onConfirm = { confirmedName ->
                    scope.launch {
                        repo.updateSessionName(session.id, confirmedName)
                        renameSession = null
                    }
                }
            )
        }

    }
}

@Composable
fun RenameSessionDialog(
    currentName: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Session") },
        text = {
            TextField(
                value = currentName,
                onValueChange = onValueChange,
                singleLine = true,
                label = { Text("Session name") }
            )
        },
        confirmButton = {
            TextButton(
                enabled = currentName.isNotBlank(),
                onClick = { onConfirm(currentName.trim()) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SessionListItem(
    session: GpsSessionsEntity,
    onDeleteClick: () -> Unit,
    onItemClick: () -> Unit,
    onRenameClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(session.name, style = MaterialTheme.typography.bodyLarge)
        Row {
            IconButton(onClick = onRenameClick) {
                Icon(Icons.Default.Edit, contentDescription = "Rename")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}


@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun LiveMapWithCompass(
    points: List<LatLng>,
    segmentPaces: List<Double>,
    checkpoints: List<LatLng>,
    waypoints: List<LatLng>,
    currentLatLng: LatLng,
    settings: Utils.SessionColorSettings,
    azimuth: Float,
    sessionState: SessionState,
    cameraPositionState: CameraPositionState,
) {

    val mapUiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                compassEnabled = false,
                zoomControlsEnabled = false
            )
        )
    }
    val isLandscape = isLandscape()
    var orientationMode by remember { mutableStateOf(MapOrientationMode.COMPASS) }
    LaunchedEffect(currentLatLng, sessionState, orientationMode, azimuth) {
        if (sessionState == SessionState.RUNNING) {
            val cameraUpdate = when (orientationMode) {
                MapOrientationMode.COMPASS -> CameraUpdateFactory.newCameraPosition(
                    CameraPosition(currentLatLng, 15f, 0f, azimuth)
                )

                MapOrientationMode.CENTER -> CameraUpdateFactory.newCameraPosition(
                    CameraPosition(currentLatLng, 15f, 0f, 0f)
                )

                else -> null
            }
            cameraUpdate?.let { cameraPositionState.animate(it, 500) }
        }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
       
        LaunchedEffect(points, constraints) {
            if (points.size > 1) {
                val boundsBuilder = LatLngBounds.builder()
                points.forEach { boundsBuilder.include(it) }
                val bounds = boundsBuilder.build()

               
                val padding = (minOf(constraints.maxWidth, constraints.maxHeight) * 0.2).toInt()

               
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds, padding),
                    1000
                )
            }
        }
        GoogleMap(
            cameraPositionState = cameraPositionState,
            modifier = Modifier.fillMaxSize(),
            uiSettings = mapUiSettings
        ) {
            val segmentCount = minOf(
                points.size - 1,
                segmentPaces.size
            )
            repeat(segmentCount) { index ->
                Polyline(
                    points = listOf(points[index], points[index + 1]),
                    color = Utils.paceToColor(
                        segmentPaces[index],
                        settings.paceMin,
                        settings.paceMax
                    ),
                    width = 12f
                )
            }
            checkpoints.forEachIndexed { index, checkpoint ->
                Marker(
                    state = MarkerState(position = checkpoint),
                    title = "${index + 1}",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
                )
            }
            waypoints.lastOrNull()?.let { wp ->
                Marker(
                    state = MarkerState(position = wp),
                    title = "Last Waypoint",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }
            Marker(
                state = MarkerState(position = currentLatLng),
                title = "Current Location",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )
        }

        MapOrientationControls(
            modifier = Modifier
                .align(
                    if (isLandscape) Alignment.BottomEnd else Alignment.TopEnd
                )
                .padding(16.dp),
            initialMode = orientationMode,
            onModeChange = { newMode ->
                orientationMode = newMode
            },
            azimuth = azimuth
        )
    }
}


@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun MainScreen(
    vm: MainViewModel,
    onStartLocationService: () -> Unit,
    onReset: () -> Unit = {},
    onOpenHistory: () -> Unit
) {
    val isLandscape = isLandscape()
    val azimuth by vm.azimuth.collectAsState()
    val cameraPositionState = rememberCameraPositionState()
    val sessionState by vm.sessionState.collectAsState()
    val points by vm.trackLatLng.collectAsState()
    val segmentPaces by vm.segmentPaces.collectAsState()
    val scope = rememberCoroutineScope()
    val currentLocationValue by vm.currentLocation.collectAsState()
    val trackPoints by vm.trackLatLng.collectAsState()
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    if (showSaveDialog) {
        SaveSessionDialog(
            onDismiss = { showSaveDialog = false },
            onConfirm = { name, description ->
                vm.finishAndResetSession(name, description)
            })
    }
    val settings by vm.settings.collectAsState()
    val waypoints by vm.waypointsLatLng.collectAsState()
    val wpPathDist by vm.wpPathDist.collectAsState()
    val checkpoints by vm.checkpointsLatLng.collectAsState()
    val elapsedSec by vm.elapsedSec.collectAsState()
    val totalDist by vm.totalDistanceMeters.collectAsState()
    val cpDist by vm.distanceFromCheckpoint.collectAsState()
    val cpDirectDist by vm.distanceFromCheckpointDirect.collectAsState()
    val wpDist by vm.distanceFromWaypoint.collectAsState()
    val pace by vm.paceFromStart.collectAsState()
    val cpPace by vm.paceFromCheckpoint.collectAsState()
    val wpPace by vm.paceFromWaypoint.collectAsState()
    val preview by vm.previewLocation.collectAsState()
    val currentLatLng =
        currentLocationValue?.let { LatLng(it.latitude, it.longitude) }
            ?: preview?.let { LatLng(it.latitude, it.longitude) }
            ?: trackPoints.lastOrNull()
            ?: LatLng(59.437, 24.753)

    var showOptions by remember { mutableStateOf(false) }


    if (showOptions) {
        SessionOptionsSheet(onDismiss = { showOptions = false }) {
            Column(
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Track Coloring", style = MaterialTheme.typography.titleMedium)
                SpeedColorSettings(
                    settings = settings,
                    onChange = { newSettings ->
                        vm.updateSettings(newSettings)
                    },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sync interval (seconds)")
                    Text(
                        text = vm.tracker.syncIntervalSec.collectAsState().value.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = vm.tracker.syncIntervalSec.collectAsState().value.toFloat(),
                    valueRange = 5f..60f,
                    steps = 5,
                    onValueChange = {
                        vm.tracker.updateSyncInterval(it.toInt())
                    }
                )
            }
        }
    }
    LaunchedEffect(currentLocationValue) {
        if (sessionState == SessionState.IDLE) {
            currentLocationValue?.let {
                cameraPositionState.move(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(it.latitude, it.longitude),
                        15f
                    )
                )
            }
        }
    }
    if (isLandscape) {
        Row(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.1f)
                    .padding(vertical = 2.dp, horizontal = 2.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        when (sessionState) {
                            SessionState.IDLE -> {
                                scope.launch {
                                    vm.startDefaultSession()
                                    onStartLocationService()
                                }
                            }

                            SessionState.RUNNING -> vm.pauseSession()
                            SessionState.PAUSED -> vm.resumeSession()
                        }
                    },
                ) {
                    when (sessionState) {
                        SessionState.IDLE -> Icon(
                            painterResource(id = R.drawable.ic_start),
                            contentDescription = "Start"
                        )

                        SessionState.RUNNING -> Icon(
                            painterResource(id = R.drawable.ic_end),
                            contentDescription = "Pause"
                        )

                        SessionState.PAUSED -> Icon(
                            painterResource(id = R.drawable.ic_start),
                            contentDescription = "Resume"
                        )
                    }
                }
                Button(
                    onClick = { vm.addCheckpoint() },
                    enabled = sessionState == SessionState.RUNNING
                ) {
                    Icon(
                        painterResource(id = R.drawable.ic_checkpoint),
                        contentDescription = "Add Checkpoint"
                    )
                }
                Button(
                    onClick = { vm.addWaypoint() },
                    enabled = sessionState == SessionState.RUNNING
                ) {
                    Icon(
                        painterResource(id = R.drawable.ic_waypoint),
                        contentDescription = "Add Waypoint"
                    )
                }
            }

            Box(Modifier.weight(0.7f)) {
                LiveMapWithCompass(
                    points = points,
                    waypoints = waypoints,
                    checkpoints = checkpoints,
                    currentLatLng = currentLatLng,
                    azimuth = azimuth,
                    sessionState = sessionState,
                    cameraPositionState = cameraPositionState,
                    settings = settings,
                    segmentPaces = segmentPaces,
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                   
                    TextButton(
                        onClick = onReset,
                        enabled = sessionState != SessionState.IDLE
                    ) {
                        Text("Reset")
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_route_history),
                            contentDescription = "Open History"
                        )
                    }
                    IconButton(onClick = { showOptions = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_options),
                            contentDescription = "Options"
                        )
                    }
                }
               

            }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.2f)
                    .padding(vertical = 8.dp, horizontal = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatItem(label = "Duration", value = Utils.formatTime(elapsedSec))
                StatItem(label = "Distance", value = "${totalDist.toInt()} m")
                StatItem(label = "Avg. Pace", value = Utils.formatPace(pace))
                Spacer(Modifier.height(8.dp))
                StatItem(label = "From CP", value = "${cpDist.toInt()} m")
                StatItem(label = "Direct", value = "${cpDirectDist.toInt()} m")
                StatItem(label = "CP Pace", value = Utils.formatPace(cpPace))
                Spacer(Modifier.height(8.dp))
                StatItem(label = "From WP", value = "${wpDist.toInt()} m")
               
                StatItem(label = "Direct", value = "${cpDirectDist.toInt()} m")
                StatItem(label = "WP Pace", value = Utils.formatPace(wpPace))
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onReset) { Text("Reset") }
                IconButton(onClick = onOpenHistory) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_route_history),
                        contentDescription = "Open History"
                    )
                }
                IconButton(onClick = { showOptions = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_options),
                        contentDescription = "Options"
                    )
                }
            }
            Box(Modifier.weight(1f)) {
                LiveMapWithCompass(
                    points = points,
                    waypoints = waypoints,
                    checkpoints = checkpoints,
                    currentLatLng = currentLatLng,
                    azimuth = azimuth,
                    sessionState = sessionState,
                    cameraPositionState = cameraPositionState,
                    settings = settings,
                    segmentPaces = segmentPaces,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(onClick = {
                        when (sessionState) {
                            SessionState.IDLE -> {
                                scope.launch {
                                    vm.startDefaultSession()
                                    onStartLocationService()
                                }
                            }

                            SessionState.RUNNING -> {
                                vm.pauseSession()
                            }

                            SessionState.PAUSED -> {
                                vm.resumeSession()
                            }
                        }
                    }) {
                        Text(
                            when (sessionState) {
                                SessionState.IDLE -> "START"
                                SessionState.RUNNING -> "PAUSE"
                                SessionState.PAUSED -> "RESUME"
                            }
                        )
                    }
                    Text(Utils.formatTime(elapsedSec))
                    Text("${totalDist.toInt()} m")

                    Text("Pace: ${Utils.formatPace(pace)}")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { vm.addCheckpoint() },
                        enabled = sessionState == SessionState.RUNNING
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_checkpoint),
                            contentDescription = "Add Checkpoint"
                        )
                    }
                    Text("${cpDist.toInt()} m")
                    Text("${cpDirectDist.toInt()} m")
                    Text("Pace: ${Utils.formatPace(cpPace)}")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { vm.addWaypoint() },
                        enabled = sessionState == SessionState.RUNNING
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_waypoint),
                            contentDescription = "Add Waypoint"
                        )
                    }
                    Text("${wpDist.toInt()} m")
                    Text("${wpPathDist.toInt()} m")
                    Text("Pace: ${Utils.formatPace(wpPace)}")
                }
            }
        }
    }
}


@Composable
fun PermissionController(
    onPermissionsGranted: @Composable () -> Unit
) {
    val context = LocalContext.current

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
                Text(
                    "Allow Location Access All the Time",
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "This app tracks your activity even when closed. Please select 'Allow all the time' to enable this.",
                    textAlign = TextAlign.Center
                )
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
                Text(
                    "This app needs Location and Notification permissions to function.",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionOptionsSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val modalBottomSheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalBottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        content()
    }

}// In MainActivity.kt

@Composable
fun SpeedColorSettings(
    settings: Utils.SessionColorSettings,
    onChange: (Utils.SessionColorSettings) -> Unit,
) {
   
   
    val defaultMin = 3f
    val defaultMax = 15f
    val sanitizedPaceMax = settings.paceMax.toFloat().coerceAtMost(settings.paceMin.toFloat())
    val sanitizedPaceMin = settings.paceMin.toFloat().coerceAtLeast(settings.paceMax.toFloat())
    val sliderRange =
        minOf(defaultMin, settings.paceMax.toFloat())..maxOf(defaultMax, settings.paceMin.toFloat())

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pace Range (min/km)")
            Text(
                text = "${Utils.formatPace(settings.paceMax)} - ${Utils.formatPace(settings.paceMin)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }

        RangeSlider(
            value = sanitizedPaceMax..sanitizedPaceMin,
            onValueChange = { newRange ->
                val newPaceMax = newRange.start.coerceAtMost(newRange.endInclusive)
                val newPaceMin = newRange.endInclusive.coerceAtLeast(newRange.start)

                onChange(
                    settings.copy(
                        paceMax = newPaceMax.toDouble(),
                        paceMin = newPaceMin.toDouble()
                    )
                )
            },
            valueRange = sliderRange
        )
    }
}
