package com.example.todoaccesible.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.panelDataStore: DataStore<Preferences> by preferencesDataStore(name = "panel_admin_prefs")

/**
 * Qué secciones (por nivel) del Panel de administración están colapsadas,
 * persistido en DataStore. Equivalente a la llave de `localStorage`
 * `panel_admin_secciones_colapsadas` en la web.
 */
class PanelCollapseStore(private val context: Context) {
    companion object {
        private val COLLAPSED_KEY = stringSetPreferencesKey("panel_admin_niveles_colapsados")
    }

    val collapsed: Flow<Set<String>> = context.panelDataStore.data.map { prefs -> prefs[COLLAPSED_KEY] ?: emptySet() }

    suspend fun toggle(nivel: String) {
        context.panelDataStore.edit { prefs ->
            val current = prefs[COLLAPSED_KEY] ?: emptySet()
            prefs[COLLAPSED_KEY] = if (nivel in current) current - nivel else current + nivel
        }
    }
}
