package com.example.gpssportmap.ui.auth

import android.content.Context
import androidx.core.content.edit
import com.example.gpssportmap.utils.C
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    fun saveAppUserId(userId: String) {
        prefs.edit { putString(C.KEY_USER_ID, userId) }
    }

    fun getAppUserId(): String = prefs.getString(C.KEY_USER_ID, null)
        ?: throw IllegalStateException("User not authenticated")

    fun saveToken(token: String) {
        prefs.edit { putString("jwt", token) }
    }

    fun getToken(): String? {
        return prefs.getString("jwt", null)
    }
}