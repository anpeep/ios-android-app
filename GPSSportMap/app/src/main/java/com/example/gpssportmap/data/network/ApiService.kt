package com.example.gpssportmap.data.network

import com.example.gpssportmap.data.db.GpsLocationEntity
import com.example.gpssportmap.data.db.GpsLocationTypeEntity
import com.example.gpssportmap.data.db.GpsSessionCreateDto
import com.example.gpssportmap.data.db.GpsSessionEntity
import com.example.gpssportmap.data.db.GpsSessionTypeEntity
import com.example.gpssportmap.data.network.models.JwtResponse
import com.example.gpssportmap.data.network.models.LoginRequest
import com.example.gpssportmap.data.network.models.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @POST("account/login")
    suspend fun login(@Body req: LoginRequest): JwtResponse

    @POST("account/register")
    suspend fun register(@Body req: RegisterRequest): JwtResponse

    @GET("GpsSessionTypes")
    suspend fun getSessionTypes(): List<GpsSessionTypeEntity>

    @GET("GpsLocationTypes")
    suspend fun getLocationTypes(): List<GpsLocationTypeEntity>

    @POST("GpsSessions")
    suspend fun startSession(
        @Body dto: GpsSessionCreateDto
    ): GpsSessionEntity

    @POST("GpsLocations")
    suspend fun addLocation(
        @Body location: GpsLocationEntity
    ): GpsLocationEntity
}
