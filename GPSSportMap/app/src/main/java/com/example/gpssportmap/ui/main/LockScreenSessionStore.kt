package com.example.gpssportmap.ui.main

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class LockScreenSessionStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context
        .createDeviceProtectedStorageContext()
        .getSharedPreferences("lock_session", Context.MODE_PRIVATE)


    fun stopSession() {
        prefs.edit {
            clear()
        }
    }
}