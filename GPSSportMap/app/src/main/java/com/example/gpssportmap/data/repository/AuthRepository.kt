package com.example.gpssportmap.data.repository

import com.example.gpssportmap.data.TokenStore
import com.example.gpssportmap.data.network.ApiService
import com.example.gpssportmap.data.network.models.LoginRequest
import com.example.gpssportmap.data.network.models.RegisterRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: ApiService,
    private val tokenStore: TokenStore
) {

    fun getToken(): String? = tokenStore.getToken()

    fun requireToken(): String =
        getToken() ?: error("User not authenticated")

    suspend fun login(email: String, password: String) {
        val response = authApi.login(LoginRequest(email, password))
        tokenStore.saveToken(response.token)
    }
    suspend fun register(
        first: String,
        last: String,
        email: String,
        password: String
    ) {
        val response = authApi.register(
            RegisterRequest(email, password, first, last)
        )
        tokenStore.saveToken(response.token)
    }

    fun saveToken(token: String) =
        tokenStore.saveToken(token)

    fun logout() {
        tokenStore.clearToken()
    }
}
