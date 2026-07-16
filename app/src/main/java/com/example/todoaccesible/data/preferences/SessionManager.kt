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

/**
 * Sesión activa guardada localmente (equivalente offline a localStorage
 * ['token'] de la web). Cuando exista el backend, aquí se agregará también
 * el JWT bajo la misma llave, sin cambiar la forma en que las pantallas
 * consumen `observeSession()`.
 */
class SessionManager(private val context: Context) {
    companion object {
        private val USER_ID_KEY = longPreferencesKey("session_user_id")
        private val ROLE_KEY = stringPreferencesKey("session_role")
    }

    suspend fun startSession(userId: Long, rol: Role) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID_KEY] = userId
            prefs[ROLE_KEY] = rol.name
        }
    }

    suspend fun endSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(USER_ID_KEY)
            prefs.remove(ROLE_KEY)
        }
    }

    val session: Flow<UserSession?> = context.dataStore.data.map { prefs ->
        val userId = prefs[USER_ID_KEY]
        val rol = prefs[ROLE_KEY]
        if (userId != null && rol != null) UserSession(userId, Role.valueOf(rol)) else null
    }
}
