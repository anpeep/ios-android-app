package com.example.gpssportmap.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.gpssportmap.R
import com.example.gpssportmap.domain.model.SessionTracker
import com.example.gpssportmap.ui.main.LockScreenSessionStore
import com.example.gpssportmap.ui.main.MainActivity
import com.example.gpssportmap.utils.C
import com.example.gpssportmap.utils.C.ACTION_CP
import com.example.gpssportmap.utils.C.ACTION_PREVIEW
import com.example.gpssportmap.utils.C.ACTION_START
import com.example.gpssportmap.utils.C.ACTION_STOP
import com.example.gpssportmap.utils.C.ACTION_WP
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : LifecycleService() {

    @Inject lateinit var tracker: SessionTracker
    @Inject lateinit var notificationManager: NotificationManager
    @Inject lateinit var lockStore: LockScreenSessionStore

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback


    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var uiJob: Job? = null
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()

        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        registerReceiver(
            stopReceiver,
            IntentFilter("STOP_CONFIRMED"),
            RECEIVER_NOT_EXPORTED
        )

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    // --- FIX STARTS HERE ---
                    // Launch a coroutine on the service's scope to call the suspend function
                    serviceScope.launch {
                        tracker.onLocation(location)

                    }
                }
            }
        }}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> {
                serviceScope.launch {
                    tracker.startSession(
                        name = "Session",
                        description = null,
                        sessionTypeId = "DEFAULT"
                    )
                }
                start()
            }
            ACTION_PREVIEW -> {
                start() // preview still needs GPS
            }
            ACTION_STOP -> stop()

            ACTION_CP -> serviceScope.launch {
                tracker.addCheckpoint()
            }

            ACTION_WP -> serviceScope.launch {
                tracker.addWaypoint()
            }
        }
        return START_STICKY
    }


    private fun start() {
        if (isRunning) return
        isRunning = true

        startForeground(
            C.NOTIFICATION_ID,
            createNotification(0, "00:00")
        )

        startLocationUpdates()
    }

    private fun stop() {
        if (!isRunning) return
        isRunning = false

        fusedClient.removeLocationUpdates(locationCallback)
        uiJob?.cancel()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000L
        ).setMinUpdateIntervalMillis(1000)
            .build()

        fusedClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun createNotification(distance: Int, time: String): Notification {
        createNotificationChannel()

        val remote = RemoteViews(packageName, R.layout.notification_tracking).apply {
            setTextViewText(R.id.tv_distance, "Distance: ${distance}m")
            setTextViewText(R.id.tv_time, "Time: $time")
            setOnClickPendingIntent(R.id.btn_wp, pending(ACTION_WP))
            setOnClickPendingIntent(R.id.btn_cp, pending(ACTION_CP))
        }

        return NotificationCompat.Builder(this, C.NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_compass)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remote)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent())
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    C.NOTIFICATION_CHANNEL,
                    "Tracking",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }

    private fun pending(action: String): PendingIntent =
        PendingIntent.getService(
            this,
            action.hashCode(),
            Intent(this, LocationService::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun contentIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "STOP_CONFIRMED") stop()
        }
    }

    override fun onDestroy() {
        unregisterReceiver(stopReceiver)
        serviceScope.cancel()
        super.onDestroy()
    }
}
