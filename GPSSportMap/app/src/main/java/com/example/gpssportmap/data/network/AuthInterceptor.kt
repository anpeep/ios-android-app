package com.example.gpssportmap.data.network

import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val prefs: SharedPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = prefs.getString("jwt_token", null)

        val req = chain.request().newBuilder()
            .addHeader("Content-Type", "application/json")
            .apply {
                if (token != null) {
                    addHeader("Authorization", "Bearer $token")
                }
            }
            .build()

        return chain.proceed(req)
    }
}
