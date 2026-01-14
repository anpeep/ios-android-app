package com.example.gpssportmap.domain

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.location.Location
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.gpssportmap.R
import com.example.gpssportmap.data.LocationSyncWorker
import com.example.gpssportmap.ui.auth.TokenStore
import com.example.gpssportmap.ui.main.MainActivity
import com.example.gpssportmap.utils.C
import com.example.gpssportmap.utils.Utils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class NotificationService : LifecycleService() {
    @Inject
    lateinit var tokenStore: TokenStore

    @Inject
    lateinit var tracker: SessionTracker

    @Inject
    lateinit var notificationManager: NotificationManager

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var lastLocation: Location? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var uiJob: Job? = null
    private var isRunning = false


    override fun onCreate() {
        super.onCreate()

        val channel = NotificationChannel(
            C.NOTIFICATION_CHANNEL,
            "Location Tracking",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.lastOrNull()?.let { location ->
                    val previousLocation = lastLocation
                    if (previousLocation == null || isValidLocation(previousLocation, location)) {
                        serviceScope.launch {
                            tracker.onLocationUpdate(location)
                        }
                    }
                    lastLocation = location
                }
            }
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(C.NOTIFICATION_ID, createNotification(null, null))
        if (intent?.action == "ACTION_STOP") {
            stop()
            return START_NOT_STICKY
        }
        when (intent?.action) {

            C.ACTION_START -> start()

            C.ACTION_ADD_CP -> {
                if (isRunning) {
                    serviceScope.launch {
                        val currentLocation = tracker.currentLocation.value
                        if (currentLocation != null) {
                            tracker.addCheckpoint(currentLocation)
                        }
                    }
                }
            }

            C.ACTION_ADD_WP -> {
                if (isRunning) {
                    serviceScope.launch {
                        val currentLocation = tracker.currentLocation.value
                        if (currentLocation != null) {
                            tracker.addWaypoint(currentLocation)
                        }
                    }
                }
            }

            "ACTION_PREVIEW" -> startLocationUpdates()

        }
        return START_STICKY
    }

    @OptIn(FlowPreview::class)
    private fun start() {
        if (isRunning) return
        isRunning = true
        lastLocation = null
        startLocationUpdates()
        scheduleLocationSync()
        uiJob?.cancel()
        uiJob = tracker.elapsedSec
            .combine(tracker.totalDistanceMeters) { time, dist ->
                createNotification(time, dist)
            }
            .sample(5000)
            .onEach { notification ->
                notificationManager.notify(C.NOTIFICATION_ID, notification)
            }
            .launchIn(serviceScope)
    }

    private fun stop() {
        if (!isRunning) return
        isRunning = false

        fusedClient.removeLocationUpdates(locationCallback)
        uiJob?.cancel()
        lastLocation = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).setMinUpdateIntervalMillis(3000)
            .build()

        fusedClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun isValidLocation(prev: Location, curr: Location): Boolean {
        if (curr.time == prev.time) return false
        val distance = prev.distanceTo(curr)
        val timeSec = (curr.time - prev.time) / 1000f
        val speed = distance / timeSec

        return speed < 15f
    }

    private fun createNotification(elapsedSeconds: Double?, totalDistance: Double?): Notification {
        ensureChannel()
        val contentText = if (elapsedSeconds != null && totalDistance != null) {
            "Time: ${Utils.formatTime(elapsedSeconds)} - Distance: ${totalDistance.toInt()} m"
        } else {
            "Recording GPS session"
        }
        return NotificationCompat.Builder(this, C.NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_compass)
            .setContentTitle("Tracking active")
            .setContentText(contentText)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_checkpoint, "CP", actionPending(C.ACTION_ADD_CP))
            .addAction(R.drawable.ic_waypoint, "WP", actionPending(C.ACTION_ADD_WP))

            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }

    private fun scheduleLocationSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request =
            PeriodicWorkRequestBuilder<LocationSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork(
                "location_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    private fun actionPending(action: String): PendingIntent =
        PendingIntent.getService(
            this,
            action.hashCode(),
            Intent(this, NotificationService::class.java).apply {
                this.action = action
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun ensureChannel() {
        val channel = NotificationChannel(
            C.NOTIFICATION_CHANNEL,
            "Tracking",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }


    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}