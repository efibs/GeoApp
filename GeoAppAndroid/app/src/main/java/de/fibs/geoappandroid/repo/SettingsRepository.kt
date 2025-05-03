package de.fibs.geoappandroid.repo

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.fibs.geoappandroid.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.Claim

class SettingsRepository private constructor(private val context: Context) {

    private val TOKEN_KEY = stringPreferencesKey("token")
    private val API_ENDPOINT_KEY = stringPreferencesKey("api_endpoint")
    private val SENSOR_FREQUENCY_KEY = longPreferencesKey("sensor_frequency")
    private val SEND_FREQUENCY_KEY = longPreferencesKey("send_frequency")

    val token: Flow<String> = context.dataStore.data
        .map {preferences ->
            preferences[TOKEN_KEY] ?: ""
        }
    suspend fun setToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    val userId: Flow<String> = token
        .map { token ->
            try {
                getUserId(token)
            } catch (e: Exception) {
                Log.e("GeoApp", "Error while decoding token", e)
                ""
            }
        }

    val tokenHasWritePermission: Flow<Boolean> = token
        .map { token ->
            try {
                getHasWritePermission(token)
            } catch (e: Exception) {
                Log.e("GeoApp", "Error while decoding token", e)
                false
            }
        }

    val tokenExpirationDate: Flow<Date?> = token
        .map { token ->
            try {
                getExpiration(token)
            } catch (e: Exception) {
                Log.e("GeoApp", "Error while decoding token", e)
                null
            }
        }

    val tokenIsExpired: Flow<Boolean> = tokenExpirationDate
        .map { date ->
            date!! >= Date()
        }

    val apiEndpoint: Flow<String> = context.dataStore.data
        .map {preferences ->
            preferences[API_ENDPOINT_KEY] ?: DEFAULT_API_ENDPOINT
        }
    suspend fun setApiEndpoint(endpoint: String) {
        context.dataStore.edit { preferences ->
            preferences[API_ENDPOINT_KEY] = endpoint
        }
    }

    private val _collecting = MutableLiveData(false)
    val collecting: LiveData<Boolean> = _collecting

    fun setCollecting(coll: Boolean) {
        _collecting.postValue(coll)
    }

    val sensorFrequency: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[SENSOR_FREQUENCY_KEY] ?: DEFAULT_SENSOR_FREQUENCY
        }
    suspend fun setSensorFrequency(freq: Long) {
        context.dataStore.edit { preferences ->
            preferences[SENSOR_FREQUENCY_KEY] = freq
        }
    }

    val sendFrequency: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[SEND_FREQUENCY_KEY] ?: DEFAULT_SEND_FREQUENCY
        }
    suspend fun setSendFrequency(freq: Long) {
        context.dataStore.edit { preferences ->
            preferences[SEND_FREQUENCY_KEY] = freq
        }
    }

    val bufferSize = 100000
    private val _currentBufferSize = MutableLiveData(0)
    val currentBufferSize: LiveData<Int> = _currentBufferSize

    fun setCurrentBufferSize(s: Int) {
        _currentBufferSize.postValue(s)
    }

    private val _lastUpdateTime = MutableLiveData<Date?>(null)
    val lastSensorUpdateTime: LiveData<Date?> = _lastUpdateTime

    fun setLastSensorUpdateTime(newDate: Date) {
        _lastUpdateTime.postValue(newDate)
    }

    private val _lastRequestDurationMilliseconds = MutableLiveData<Long?>(null)
    val lastRequestDurationMilliseconds: LiveData<Long?> = _lastRequestDurationMilliseconds

    fun setLastRequestDurationMillis(newDuration: Long) {
        _lastRequestDurationMilliseconds.postValue(newDuration)
    }

    private fun getClaim(token: String, claimKey: String): Claim? {
        if (token.isBlank()) {
            return null
        }
        try {
            val decodedJWT = JWT.decode(token)
            val claim = decodedJWT.getClaim(claimKey) ?: throw Exception("Invalid token provided")
            return claim
        } catch (e: Exception) {
            return null
        }
    }

    private fun getUserId(token: String): String {
        val userIdClaimKey = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier"
        return getClaim(token, userIdClaimKey)?.asString() ?: "--"
    }

    private fun getExpiration(token: String): Date {
        val expirationClaimKey = "exp"
        val expirationClaim = getClaim(token, expirationClaimKey)
        val expirationDate = Date((expirationClaim?.asLong() ?: 0) * 1000)
        return expirationDate
    }

    private fun getHasWritePermission(token: String): Boolean {
        val permissionsClaimKey = "Permissions"
        val writePermission = "perm:WriteData"
        val permissionsClaim = getClaim(token, permissionsClaimKey)

        val hasWritePermission = when {
            permissionsClaim?.asString() != null -> {
                permissionsClaim.asString() == writePermission
            }
            permissionsClaim?.asList(String::class.java) != null -> {
                permissionsClaim.asList(String::class.java).contains(writePermission)
            }
            else -> false
        }

        return hasWritePermission
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context?): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                if (context == null) {
                    throw Exception("Tried to instanciate repo without context")
                }
                INSTANCE ?: SettingsRepository(context).also { INSTANCE = it }
            }
        }

        const val DEFAULT_SENSOR_FREQUENCY = 10L
        const val DEFAULT_SEND_FREQUENCY = 40L
        const val DEFAULT_API_ENDPOINT = "https://kleiner3.ddns.net:1169"
    }
}