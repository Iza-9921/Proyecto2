package com.example.todoaccesible.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow

private val Context.quotaDataStore: DataStore<Preferences> by preferencesDataStore(name = "diagnostico_quota_prefs")

/**
 * Cupo de diagnósticos disponibles por cliente, asignado manualmente por el
 * administrador (el pago se hace fuera de la app, de forma presencial). Se
 * guarda en DataStore -mismo mecanismo que ya usa [SessionManager]- para que
 * sobreviva a que se cierre la app o el proceso se reinicie; hoy no hay
 * backend ni API, así que este es el único lugar que habría que sustituir el
 * día que exista, sin tocar [com.example.todoaccesible.data.repository.UserRepository]
 * ni las pantallas que lo consumen.
 *
 * Convención de almacenamiento: si la llave no existe, el cliente aún no
 * tiene cupo asignado (0). [UNLIMITED_SENTINEL] representa "ilimitados"
 * (equivalente a `null` en el dominio).
 */
class DiagnosticQuotaStore(private val context: Context) {
    companion object {
        private const val UNLIMITED_SENTINEL = -1
    }

    private fun keyFor(userId: Long) = intPreferencesKey("quota_$userId")

    val preferences: Flow<Preferences> get() = context.quotaDataStore.data

    fun read(prefs: Preferences, userId: Long): Int? =
        when (val stored = prefs[keyFor(userId)]) {
            null -> 0
            UNLIMITED_SENTINEL -> null
            else -> stored
        }

    suspend fun set(userId: Long, cantidad: Int?) {
        context.quotaDataStore.edit { prefs ->
            prefs[keyFor(userId)] = cantidad?.coerceAtLeast(0) ?: UNLIMITED_SENTINEL
        }
    }

    suspend fun decrement(userId: Long) {
        context.quotaDataStore.edit { prefs ->
            val current = prefs[keyFor(userId)]
            if (current != null && current != UNLIMITED_SENTINEL && current > 0) {
                prefs[keyFor(userId)] = current - 1
            }
        }
    }

    /** Siembra un valor inicial solo si el cliente todavía no tiene nada guardado (no pisa asignaciones previas del admin). */
    suspend fun seedIfAbsent(userId: Long, valorPorDefecto: Int?) {
        context.quotaDataStore.edit { prefs ->
            if (prefs[keyFor(userId)] == null) {
                prefs[keyFor(userId)] = valorPorDefecto?.coerceAtLeast(0) ?: UNLIMITED_SENTINEL
            }
        }
    }
}
