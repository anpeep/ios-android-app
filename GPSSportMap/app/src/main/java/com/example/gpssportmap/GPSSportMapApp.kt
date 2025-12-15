package com.example.gpssportmap

import android.app.Application
import com.example.gpssportmap.data.network.LifecycleManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GPSSportMapApp : Application() {
    @Inject
    lateinit var lifecycleManager: LifecycleManager // <-- Inject the manager

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(lifecycleManager) // <-- Register it
    }

}