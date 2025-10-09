package com.example.kaver

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
class DemoService : Service() {
    private val scheduledExecutorService = Executors.newScheduledThreadPool(1)
    private var startTimeMillis: Long = 0
    private var running = false

    override fun onBind(p0: Intent?): IBinder? = null

    companion object {
        private val TAG = DemoService::class.java.simpleName
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
    }

    override fun onDestroy() {
        super.onDestroy()
        scheduledExecutorService.shutdown()
        Log.d(TAG, "onDestroy")
    }

    fun startBroadcast() {
        if (running) return
        startTimeMillis = System.currentTimeMillis()
        running = true
        scheduledExecutorService.scheduleWithFixedDelay(
            {
                if (!running) return@scheduleWithFixedDelay

                val elapsedMillis = System.currentTimeMillis() - startTimeMillis
                val seconds = (elapsedMillis / 1000) % 60
                val minutes = (elapsedMillis / (1000 * 60)) % 60
                val hours = (elapsedMillis / (1000 * 60 * 60))

                val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                Log.d(TAG, "Timer: $timeFormatted")

                val intent = Intent(C.IntentBackgroundServiceTime)
                intent.putExtra(C.IntentBackgroundServiceTimePayload, timeFormatted)
                LocalBroadcastManager
                    .getInstance(applicationContext).sendBroadcast(intent)
            },
            0, // initialDelay
            1, // iga sekundi tagant
            TimeUnit.SECONDS
        )
    }
    fun stopTimer() {
        running = false
    }

    fun resetTimer() {
        startTimeMillis = System.currentTimeMillis()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        startBroadcast()
        return START_STICKY
    }
}
