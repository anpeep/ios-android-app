package com.example.gpssportmap.data.repository

import android.util.Log
import androidx.room.Transaction
import com.example.gpssportmap.data.db.dao.GpsLocationTypeDao
import com.example.gpssportmap.data.db.dao.GpsLocationsDao
import com.example.gpssportmap.data.db.dao.GpsSessionTypeDao
import com.example.gpssportmap.data.db.dao.GpsSessionsDao
import com.example.gpssportmap.data.db.entities.GpsLocationsEntity
import com.example.gpssportmap.data.db.entities.GpsSessionsEntity
import com.example.gpssportmap.data.mappers.toEntity
import com.example.gpssportmap.data.network.ApiService
import com.example.gpssportmap.data.network.dtos.GpsLocationCreateDto
import com.example.gpssportmap.data.network.dtos.GpsSessionsCreateDto
import com.example.gpssportmap.data.network.dtos.GpsSessionsUpdateDto
import com.example.gpssportmap.data.network.dtos.LoginRequest
import com.example.gpssportmap.data.network.dtos.RegisterRequest
import com.example.gpssportmap.ui.auth.TokenStore
import com.example.gpssportmap.utils.C
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val api: ApiService,
    private val sessionDao: GpsSessionsDao,
    private val locationDao: GpsLocationsDao,
    private val locationTypeDao: GpsLocationTypeDao,
    val sessionTypeDao: GpsSessionTypeDao,
    private val tokenStore: TokenStore,
) {

    fun isLoggedIn(): Boolean {
        return !tokenStore.getToken().isNullOrEmpty()
    }

    suspend fun initializeIfNeeded() {
        sessionTypeDao.insertAll(api.getSessionTypes().map { it.toEntity() })
        locationTypeDao.insertAll(api.getGpsLocationTypes().map { it.toEntity() })
    }


    suspend fun startSession(): String {
        Log.e("DEBUG", "LocationType count = ${locationTypeDao.count()}")

        val dto = GpsSessionsCreateDto(
            name = "Running session",
            description = "Auto started",
            gpsSessionTypeId = C.SESSION_TYPE_RUNNING,
            recordedAt = Instant.now().toString(),
            paceMin = null,
            paceMax = null
        )

        val response = api.startSession(dto)
        sessionDao.upsert(response.toEntity())
        tokenStore.saveAppUserId(response.appUserId)

        return response.id
    }


    suspend fun login(email: String, password: String) {
        val response = api.login(LoginRequest(email, password))
        tokenStore.saveToken(response.token)

    }

    suspend fun register(
        first: String, last: String, email: String, password: String
    ) {
        val response = api.register(RegisterRequest(email, password, first, last))
        tokenStore.saveToken(response.token)
    }
// In SessionRepository.kt

    @Transaction
    suspend fun syncSessionLocations(sessionId: String) {
        // FIX: The entire function body is now inside withContext(Dispatchers.IO)
        withContext(Dispatchers.IO) {
            // Step 1: Ensure parent entities (types) exist first.
            // This fetches types from the API and inserts them if the table is empty.
            if (sessionTypeDao.count() == 0) { // Now runs on IO thread
                val remoteSessionTypes = api.getSessionTypes()
                sessionTypeDao.insertAll(remoteSessionTypes.map { it.toEntity() })
            }
            if (locationTypeDao.count() == 0) { // Now runs on IO thread
                val remoteLocationTypes = api.getGpsLocationTypes()
                locationTypeDao.insertAll(remoteLocationTypes.map { it.toEntity() })
            }

            // Step 2: Now that types are guaranteed to exist, fetch and insert the session.
            val remoteSession = api.getSession(sessionId)
            sessionDao.update(
                remoteSession.toEntity(tokenStore.getAppUserId())
            )

            // Step 3: Finally, fetch and insert the locations for that session.
            val remoteLocations = api.getLocationsForSession(sessionId)
            locationDao.deleteForSession(sessionId)

            locationDao.insertAll(
                remoteLocations.map { it.toEntity().copy(synced = true) }
            )

        } // This brace correctly closes withContext
    } // This brace correctly closes the function


    fun requireToken(): String =
        tokenStore.getToken() ?: error("User not authenticated")

    suspend fun updateSession(sessionId: String, name: String, description: String) {
        val session = sessionDao.getSessionById(sessionId)
        Log.d("SESSION update", session.toString())

        val dto = GpsSessionsUpdateDto(
            id = session.id,
            name = name,
            description = description,
            gpsSessionTypeId = C.SESSION_TYPE_RUNNING,
            recordedAt = session.recordedAt,
            paceMin = session.paceMin,
            paceMax = session.paceMax
        )

        withContext(Dispatchers.IO) {
            try {
                api.updateSession(sessionId, dto)
            } catch (e: Exception) {
                Log.e("FinishSession", "Failed to update session on server: ${e.message}")
            }
        }
        sessionDao.update(
            session.copy(
                name = name,
                description = description
            )
        )
    }

    suspend fun deleteSession(sessionId: String) {
        requireToken()
        sessionDao.delete(sessionId)
    }

    suspend fun addLocation(
        sessionId: String,
        dto: GpsLocationCreateDto,
        gpsLocationTypeId: String
    ) {
        withContext(Dispatchers.IO) { // Run network call on a background thread
            try {
                api.addLocation(sessionId, dto)
            } catch (e: Exception) {
                // Log the error instead of ignoring it. This will help debug network issues.
                Log.e("SessionRepository", "Failed to add location to server: ${e.message}")
            }
        }

        locationDao.insert(
            dto.toEntity(
                gpsSessionId = sessionId,
                appUserId = tokenStore.getAppUserId(),
                gpsLocationTypeId = gpsLocationTypeId
            )
        )
    }

    suspend fun updateSessionName(sessionId: String, newName: String) {
        val session = sessionDao.getSessionById(sessionId)

        val dto = GpsSessionsUpdateDto(
            id = session.id,
            name = newName,
            description = session.description,
            gpsSessionTypeId = session.gpsSessionTypeId,
            recordedAt = session.recordedAt,
            paceMin = session.paceMin,
            paceMax = session.paceMax
        )
        api.updateSession(sessionId, dto)

        sessionDao.update(session.copy(name = newName))
    }
// In SessionRepository.kt

    suspend fun finishSession(
        sessionId: String,
        duration: Double,
        distance: Double,
        avgSpeed: Double
    ) {
        // --- START: New logic to update the server ---

        // 1. Get the current session details from the local database
        val session = sessionDao.getSessionById(sessionId)

        // 2. Create the Data Transfer Object (DTO) with the final stats
        val dto = GpsSessionsUpdateDto(
            id = session.id,
            name = session.name, // Keep existing name
            description = session.description, // Keep existing description
            gpsSessionTypeId = session.gpsSessionTypeId,
            recordedAt = session.recordedAt,
            paceMin = if (distance > 0) (duration / 60.0) / (distance / 1000.0) else null,

            // You don't have a final max pace, so sending null is correct.
            paceMax = null  // You can add logic for paceMax if you track it
        )
        withContext(Dispatchers.IO) {
            try {
                api.updateSession(sessionId, dto)
            } catch (e: Exception) {
                Log.e("FinishSession", "Failed to update session on server: ${e.message}")
            }
        }
        sessionDao.updateStats(
            sessionId = sessionId,
            duration = duration,
            distance = distance,
            speed = avgSpeed
        )
    }


    fun getSessionCheckpoints(sessionId: String): Flow<List<LatLng>> =
        locationDao.getLocationsByType(sessionId, C.LOCATION_TYPE_CP)
            .map { it.map { loc -> LatLng(loc.latitude, loc.longitude) } }

    fun getSessionWaypoints(sessionId: String): Flow<List<LatLng>> =
        locationDao.getLocationsByType(sessionId, C.LOCATION_TYPE_WP)
            .map { it.map { loc -> LatLng(loc.latitude.toDouble(), loc.longitude.toDouble()) } }

    fun getLocationsForSession(sessionId: String): Flow<List<GpsLocationsEntity>> {
        return locationDao.getLocationsForSession(sessionId)
    }

    fun getSessionByIdFlow(sessionId: String): Flow<GpsSessionsEntity?> {
        return sessionDao.getSessionByIdFlow(sessionId)
    }

    suspend fun getLastKnownLatLng(): LatLng? {
        return locationDao.getLastLocation()?.let {
            LatLng(it.latitude.toDouble(), it.longitude.toDouble())
        }
    }

    fun getAllSessions(): Flow<List<GpsSessionsEntity>> = sessionDao.getAllSessions()
}

