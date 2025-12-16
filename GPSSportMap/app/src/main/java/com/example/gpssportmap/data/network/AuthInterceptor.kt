package com.example.gpssportmap.data.network

import android.content.SharedPreferences
import android.util.Log
import com.example.gpssportmap.data.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.getToken()

        Log.d("AuthInterceptor", "JWT = ${token?.take(20)}")

        val request = chain.request().newBuilder()
            .addHeader("Content-Type", "application/json")
            .apply {
                if (!token.isNullOrBlank()) {
                    addHeader("Authorization", "Bearer $token")
                }
            }
            .build()

        return chain.proceed(request)
    }

}
