package com.example.gpssportmap.data.repository
import com.example.gpssportmap.data.db.GpsLocationDao
import com.example.gpssportmap.data.db.GpsLocationEntity
import com.example.gpssportmap.data.db.GpsSessionCreateDto
import com.example.gpssportmap.data.db.GpsSessionDao
import com.example.gpssportmap.data.db.GpsSessionEntity
import com.example.gpssportmap.data.db.GpsSessionTypeEntity
import com.example.gpssportmap.data.network.ApiService
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val api: ApiService,
    private val sessionDao: GpsSessionDao,
    private val locationDao: GpsLocationDao,
    private val authRepository: AuthRepository
) {

    private var activeSessionId: String? = null
    suspend fun startSession(dto: GpsSessionCreateDto): GpsSessionEntity {
        authRepository.requireToken()

        val created = api.startSession(dto)
        sessionDao.insertSession(created)

        activeSessionId = created.id.toString()
        return created
    }

    fun getActiveSessionId(): String? = activeSessionId

    suspend fun updateSession(sessionId: UUID, name: String, description: String?) {
        // First, you need to fetch the existing session to update it.
        val sessionToUpdate = sessionDao.getSessionById(sessionId.toString())

        if (sessionToUpdate != null) {
            // Now, create the updated session object.
            val updatedSession = sessionToUpdate.copy(
                name = name,
                description = description
            )

            // This line was causing the error and is no longer needed
            // sessionDao.updateSessionDetails(sessionId.toString(), name, description)

            authRepository.requireToken()

            sessionDao.update(updatedSession)
        } else {
            TODO()
        }
    }

    suspend fun deleteSession(sessionId: String) {
        authRepository.requireToken()
        locationDao.deleteForSession(sessionId)
        sessionDao.delete(sessionId)
    }

    suspend fun addLocation(location: GpsLocationEntity) {
        authRepository.requireToken()
        locationDao.insert(location)
        api.addLocation(location)
    }
    fun getAllSessions(): Flow<List<GpsSessionEntity>> = sessionDao.getAllSessions()
    suspend fun getSessionTypes(): List<GpsSessionTypeEntity> =
        api.getSessionTypes()


    fun observeLocations(sessionId: String): Flow<List<GpsLocationEntity>> =
        locationDao.getLocationsForSession(sessionId)
}

