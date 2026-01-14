package com.example.gpssportmap.ui.auth

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenStore: TokenStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val isAuthEndpoint =
            original.url.encodedPath.contains("/account/login") ||
                    original.url.encodedPath.contains("/account/register")

        val token = tokenStore.getToken()

        val request = if (!isAuthEndpoint && !token.isNullOrBlank()) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        return chain.proceed(request)
    }
}