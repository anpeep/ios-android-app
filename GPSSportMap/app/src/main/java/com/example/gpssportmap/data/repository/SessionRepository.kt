package com.example.gpssportmap.data.repository
import com.example.gpssportmap.data.TokenStore
import com.example.gpssportmap.data.db.GpsLocationDao
import com.example.gpssportmap.data.db.GpsLocationEntity
import com.example.gpssportmap.data.db.GpsSessionCreateDto
import com.example.gpssportmap.data.db.GpsSessionDao
import com.example.gpssportmap.data.db.GpsSessionEntity
import com.example.gpssportmap.data.db.GpsSessionTypeEntity
import com.example.gpssportmap.data.network.ApiService
import com.example.gpssportmap.data.network.models.LoginRequest
import com.example.gpssportmap.data.network.models.RegisterRequest
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val api: ApiService,
    private val sessionDao: GpsSessionDao,
    private val locationDao: GpsLocationDao,
    private val tokenStore: TokenStore

) {

    private var activeSessionId: String? = null
    suspend fun startSession(dto: GpsSessionCreateDto): GpsSessionEntity {
        return try {
            val created = api.startSession(dto)
            sessionDao.insertSession(created)
            activeSessionId = created.id.toString()
            created
        } catch (e: HttpException) {
            if (e.code() == 401) {
                tokenStore.clearToken()  // clear token here
            }
            throw e  // rethrow for upper layers
        }
    }


    fun getToken(): String? = tokenStore.getToken()

    fun requireToken(): String =
        getToken() ?: error("User not authenticated")

    suspend fun login(email: String, password: String) {
        val response = api.login(LoginRequest(email, password))
        tokenStore.saveToken(response.token)
    }
    suspend fun register(
        first: String,
        last: String,
        email: String,
        password: String
    ) {
        val response = api.register(
            RegisterRequest(email, password, first, last)
        )
        tokenStore.saveToken(response.token)
    }

    fun saveToken(token: String) =
        tokenStore.saveToken(token)

    fun logout() {
        tokenStore.clearToken()
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

            requireToken()

            sessionDao.update(updatedSession)
        } else {
            TODO()
        }
    }

    suspend fun deleteSession(sessionId: String) {
        requireToken()
        locationDao.deleteForSession(sessionId)
        sessionDao.delete(sessionId)
    }

    suspend fun addLocation(location: GpsLocationEntity) {
        requireToken()
        locationDao.insert(location)
        api.addLocation(location)
    }
    fun getAllSessions(): Flow<List<GpsSessionEntity>> = sessionDao.getAllSessions()
    suspend fun getSessionTypes(): List<GpsSessionTypeEntity> =
        api.getSessionTypes()


    fun observeLocations(sessionId: String): Flow<List<GpsLocationEntity>> =
        locationDao.getLocationsForSession(sessionId)
}

