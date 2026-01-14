// In: app/src/main/java/com/example/gpssportmap/data/repository/SettingsRepository.kt

package com.example.gpssportmap.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.gpssportmap.coroutines.IoDispatcher
import com.example.gpssportmap.coroutines.NormalSharedPreferences
import com.example.gpssportmap.utils.Utils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @NormalSharedPreferences private val prefs: SharedPreferences,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val KEY_PACE_MIN = "pace_min"
        private const val KEY_PACE_MAX = "pace_max"
    }

    private val _settingsFlow = MutableStateFlow(getCurrentSettings())
    val settingsFlow: Flow<Utils.SessionColorSettings> = _settingsFlow

    private fun getCurrentSettings(): Utils.SessionColorSettings {
       
        val paceMin = prefs.getString(KEY_PACE_MIN, "7.0")?.toDoubleOrNull() ?: 7.0
        val paceMax = prefs.getString(KEY_PACE_MAX, "4.0")?.toDoubleOrNull() ?: 4.0
        return Utils.SessionColorSettings(paceMin = paceMin, paceMax = paceMax)
    }

    suspend fun updateSettings(newSettings: Utils.SessionColorSettings) {
       
        if (_settingsFlow.value == newSettings) return

       
        withContext(ioDispatcher) {
            prefs.edit {
               
                putString(KEY_PACE_MIN, newSettings.paceMin.toString())
                putString(KEY_PACE_MAX, newSettings.paceMax.toString())
            }
        }

        _settingsFlow.value = newSettings
    }
}
