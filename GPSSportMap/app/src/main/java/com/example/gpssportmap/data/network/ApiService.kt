package com.example.gpssportmap.data.network

import com.example.gpssportmap.data.network.dtos.GpsLocationCreateDto
import com.example.gpssportmap.data.network.dtos.GpsLocationResponseDto
import com.example.gpssportmap.data.network.dtos.GpsLocationTypeResponseDto
import com.example.gpssportmap.data.network.dtos.GpsLocationUploadResponseDto
import com.example.gpssportmap.data.network.dtos.GpsSessionTypeResponseDto
import com.example.gpssportmap.data.network.dtos.GpsSessionsCreateDto
import com.example.gpssportmap.data.network.dtos.GpsSessionsUpdateDto
import com.example.gpssportmap.data.network.dtos.JwtResponse
import com.example.gpssportmap.data.network.dtos.LoginRequest
import com.example.gpssportmap.data.network.dtos.RegisterRequest
import com.example.gpssportmap.data.network.dtos.SessionCreateResponseDto
import com.example.gpssportmap.data.network.dtos.SessionResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @GET("GpsLocations/Session/{gpsSessionId}")
    suspend fun getLocationsForSession(
        @Path("gpsSessionId") gpsSessionId: String
    ): List<GpsLocationResponseDto>

    @GET("GpsSessions/{id}")
    suspend fun getSession(
        @Path("id") id: String
    ): SessionResponseDto


    @POST("Account/Login")
    suspend fun login(@Body req: LoginRequest): JwtResponse

    @POST("account/register")
    suspend fun register(@Body req: RegisterRequest): JwtResponse

    @POST("GpsLocations/{gpsSessionId}")
    suspend fun addLocation(
        @Path("gpsSessionId") sessionId: String,
        @Body dto: GpsLocationCreateDto
    ): GpsLocationUploadResponseDto

    @POST("GpsSessions")
    suspend fun startSession(
        @Body dto: GpsSessionsCreateDto
    ): SessionCreateResponseDto


    @PUT("GpsSessions/{id}")
    suspend fun updateSession(
        @Path("id") id: String,
        @Body body: GpsSessionsUpdateDto
    ): Response<Unit>

    @GET("GpsLocationTypes")
    suspend fun getGpsLocationTypes(): List<GpsLocationTypeResponseDto>

    @GET("GpsSessionTypes")
    suspend fun getSessionTypes(): List<GpsSessionTypeResponseDto>
}