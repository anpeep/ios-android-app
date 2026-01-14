package com.example.gpssportmap.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LifecycleManager @Inject constructor() : Application.ActivityLifecycleCallbacks {
    private val _isAppInForeground = MutableStateFlow(false)
    private var activityCount = 0

    override fun onActivityStarted(activity: Activity) {
        if (activityCount == 0) {
            _isAppInForeground.value = true
        }
        activityCount++
    }

    override fun onActivityStopped(activity: Activity) {
        activityCount--
        if (activityCount == 0) {
            _isAppInForeground.value = false
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {} // No longer needed
    override fun onActivityPaused(activity: Activity) {} // No longer needed
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}