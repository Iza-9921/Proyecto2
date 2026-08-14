package com.example.todoaccesible.data.remote

import com.example.todoaccesible.data.preferences.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Mientras haya sesión activa, llama a `POST /auth/heartbeat` cada ~10 min
 * para renovar el access token (15 min de vida) sin esperar a que expire de
 * verdad. Alcance foreground-only: se arranca/para desde [com.example.todoaccesible.AppContainer]
 * según haya o no sesión, no hay WorkManager ni servicio en background.
 */
class HeartbeatManager(
    private val authApi: AuthApiService,
    private val sessionManager: SessionManager
) {
    companion object {
        private const val INTERVAL_MS = 10 * 60 * 1000L
    }

    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                delay(INTERVAL_MS)
                try {
                    val tokens = sessionManager.tokens.first() ?: continue
                    val response = authApi.heartbeat()
                    sessionManager.updateTokens(response.token, tokens.refreshToken)
                } catch (_: Exception) {
                    // Sin conexión o token ya inválido: se reintenta en el siguiente ciclo.
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
