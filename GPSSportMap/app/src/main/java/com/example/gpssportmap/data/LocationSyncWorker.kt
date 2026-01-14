package com.example.gpssportmap.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.gpssportmap.data.db.dao.GpsLocationsDao
import com.example.gpssportmap.data.mappers.toDto
import com.example.gpssportmap.data.network.ApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LocationSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val api: ApiService,
    private val dao: GpsLocationsDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val pending = dao.getUnsynced()
        if (pending.isEmpty()) return Result.success()

        pending.forEach { loc ->
            try {
                api.addLocation(loc.gpsSessionId, loc.toDto())
                dao.markSynced(loc.id!!)
            } catch (e: Exception) {

            }
        }
        return Result.success()
    }
}