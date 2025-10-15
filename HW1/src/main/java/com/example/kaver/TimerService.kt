package com.example.kaver

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class TimerService : Service() {  // Coroutine > ExecutorService. Service + Binder > BroadcastReceiver when 1 activity
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())  // coutines line in scoper. Dispatchers.Main = works in UI thread. SupervisorJob continue when -1 cor
    private var timerJob: Job? = null  // coroutine ticket: is it active, canceled, killed? restart
    private var elapsedTimeSeconds = 0L
    private val _timeFlow = MutableStateFlow("00:00")  // change
    val timeFlow: StateFlow<String> = _timeFlow  // read

    companion object {  // without CO they are static
        private val TAG = TimerService::class.java.simpleName  // TimerService for logs
        const val ACTION_START_FRESH = "ACTION_START_FRESH"
        const val ACTION_RESUME = "ACTION_RESUME"
    }

    inner class TimerBinder : Binder() {
        val service: TimerService  // TimeBinder is inner class and 'this' is Binder object
            get() = this@TimerService  // @ to get TimerService
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "Service is bound.")
        if (timerJob == null && elapsedTimeSeconds > 0) {
            resumeTimer()
        }
        return TimerBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "Unbound from service, stopping timer coroutine.")
        timerJob?.cancel()
        timerJob = null
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FRESH -> startTimerFresh()
            ACTION_RESUME -> resumeTimer()
        }
        return START_NOT_STICKY  // timer only works when active game, don't start automatically
    }

    fun startTimerFresh() {
        if (timerJob?.isActive == true) timerJob?.cancel()
        Log.d(TAG, "Starting timer from fresh.")
        elapsedTimeSeconds = 0L
        _timeFlow.value = formatTime(0)
        resumeTimer()
    }

    private fun resumeTimer() {
        if (timerJob?.isActive == true) return
        Log.d(TAG, "Resuming timer from $elapsedTimeSeconds seconds.")
        timerJob = serviceScope.launch {
            while (true) {
                _timeFlow.value = formatTime(elapsedTimeSeconds)
                delay(1000)
                elapsedTimeSeconds++
            }
        }
    }

    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }

    override fun onDestroy() {  // we have service for onStart, onResume, onPause, onStop
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        serviceScope.cancel()  // kill coroutine
    }

}
