package com.example.gpssportmap.data.network

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LifecycleManager @Inject constructor() : Application.ActivityLifecycleCallbacks {

    // --- FIX 1: Make the StateFlow the single source of truth ---
    private val _isAppInForeground = MutableStateFlow(false)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    private var activityCount = 0

    override fun onActivityStarted(activity: Activity) {
        if (activityCount == 0) {
            _isAppInForeground.value = true // App is entering the foreground
        }
        activityCount++
    }

    override fun onActivityStopped(activity: Activity) {
        activityCount--
        if (activityCount == 0) {
            _isAppInForeground.value = false // App is entering the background
        }
    }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {} // No longer needed
    override fun onActivityPaused(activity: Activity) {} // No longer needed
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
