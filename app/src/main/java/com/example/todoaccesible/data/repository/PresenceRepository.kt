package com.example.todoaccesible.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * "Quién más está revisando este diagnóstico ahora mismo", equivalente a
 * `src/hooks/usePresence.js` en la web (heartbeat cada 30s, se considera
 * inactivo pasados 60s sin latido).
 */
interface PresenceRepository {
    /** Nombres de otros usuarios (no [excludeUserId]) con latido reciente en este diagnóstico. */
    fun observeViewers(diagnosticId: Long, excludeUserId: Long): Flow<List<String>>

    suspend fun heartbeat(diagnosticId: Long, userId: Long, userName: String)

    /** Se llama al salir de la pantalla para no esperar a que expire el latido. */
    suspend fun clear(diagnosticId: Long, userId: Long)
}
