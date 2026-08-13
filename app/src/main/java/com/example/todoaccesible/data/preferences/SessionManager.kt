package com.example.todoaccesible.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.todoaccesible.data.model.Role
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

data class UserSession(val userId: Long, val rol: Role)

/** Par de tokens JWT de la sesión activa (access + refresh, ambos emitidos por el backend). */
data class AuthTokens(val accessToken: String, val refreshToken: String)

/**
 * Sesión activa guardada localmente en DataStore: identidad (userId/rol) más
 * el par de tokens JWT del backend real. `observeSession()`/`UserSession` se
 * mantienen sin cambios para las pantallas que ya los consumen; `tokens` es
 * un flow nuevo y separado que solo consume la capa de red
 * ([com.example.todoaccesible.data.remote.AuthInterceptor],
 * [com.example.todoaccesible.data.remote.TokenAuthenticator]).
 */
class SessionManager(private val context: Context) {
    companion object {
        private val USER_ID_KEY = longPreferencesKey("session_user_id")
        private val ROLE_KEY = stringPreferencesKey("session_role")
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("session_access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("session_refresh_token")
    }

    suspend fun startSession(userId: Long, rol: Role, accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID_KEY] = userId
            prefs[ROLE_KEY] = rol.name
            prefs[ACCESS_TOKEN_KEY] = accessToken
            prefs[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = accessToken
            prefs[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun endSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(USER_ID_KEY)
            prefs.remove(ROLE_KEY)
            prefs.remove(ACCESS_TOKEN_KEY)
            prefs.remove(REFRESH_TOKEN_KEY)
        }
    }

    val session: Flow<UserSession?> = context.dataStore.data.map { prefs ->
        val userId = prefs[USER_ID_KEY]
        val rol = prefs[ROLE_KEY]
        if (userId != null && rol != null) UserSession(userId, Role.valueOf(rol)) else null
    }

    val tokens: Flow<AuthTokens?> = context.dataStore.data.map { prefs ->
        val access = prefs[ACCESS_TOKEN_KEY]
        val refresh = prefs[REFRESH_TOKEN_KEY]
        if (access != null && refresh != null) AuthTokens(access, refresh) else null
    }
}
