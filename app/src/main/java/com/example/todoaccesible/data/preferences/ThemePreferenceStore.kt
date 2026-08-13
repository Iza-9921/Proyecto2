package com.example.todoaccesible.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

enum class ThemePreference { LIGHT, DARK, SYSTEM }

/**
 * Preferencia de tema claro/oscuro, persistida en DataStore (mismo
 * mecanismo que [SessionManager]) para sobrevivir a
 * que se cierre la app. Equivalente offline a `localStorage['theme']` en la
 * web (`ThemeContext.jsx`): por defecto sigue el tema del sistema hasta que
 * el usuario toca el botón de alternar, momento en que se guarda una
 * elección explícita.
 */
class ThemePreferenceStore(private val context: Context) {
    companion object {
        private val THEME_KEY = stringPreferencesKey("theme_preference")
    }

    val preference: Flow<ThemePreference> = context.themeDataStore.data.map { prefs ->
        prefs[THEME_KEY]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() } ?: ThemePreference.SYSTEM
    }

    suspend fun set(preference: ThemePreference) {
        context.themeDataStore.edit { prefs -> prefs[THEME_KEY] = preference.name }
    }
}
