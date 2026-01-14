package com.example.gpssportmap

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.gpssportmap.data.repository.SessionRepository
import com.example.gpssportmap.utils.LifecycleManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GPSSportMapApp : Application(), Configuration.Provider {
    @Inject
    lateinit var lifecycleManager: LifecycleManager

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var referenceRepo: SessionRepository

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(lifecycleManager)
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}
