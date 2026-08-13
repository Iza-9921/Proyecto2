package com.example.todoaccesible.data.remote

import com.example.todoaccesible.data.preferences.SessionManager
import com.example.todoaccesible.data.remote.dto.AuthResponseDto
import com.example.todoaccesible.data.remote.dto.RefreshRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Ante un 401, refresca el access token con `POST /auth/refresh` (usando
 * [authApiPlain], un Retrofit SIN este mismo authenticator, para no
 * recursar) y reintenta la request original. Si el refresh falla, no
 * reintenta: deja que el 401 se propague (lo traduce
 * [com.example.todoaccesible.data.remote.ApiErrorMapper] a un logout
 * forzado). Protegido contra refrescos concurrentes con un [Mutex]
 * single-flight: solo una corrutina refresca a la vez, las demás esperan y
 * reusan el resultado.
 */
class TokenAuthenticator(
    private val sessionManager: SessionManager,
    private val authApiPlain: AuthApiService,
    private val onAuthRefreshed: (AuthResponseDto) -> Unit
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseChainLength(response) >= 3) return null
        if (response.request.url.encodedPath.contains("auth/refresh")) return null

        return runBlocking {
            mutex.withLock {
                val current = sessionManager.tokens.first() ?: return@withLock null
                val failedAuthHeader = response.request.header("Authorization")

                // Si el token que falló ya no es el actual, otra corrutina ya
                // refrescó mientras esperábamos el lock: solo reintenta con el nuevo.
                if (failedAuthHeader != null && failedAuthHeader != "Bearer ${current.accessToken}") {
                    return@withLock response.request.newBuilder()
                        .header("Authorization", "Bearer ${current.accessToken}")
                        .build()
                }

                try {
                    val refreshed = authApiPlain.refresh(RefreshRequest(current.refreshToken))
                    sessionManager.updateTokens(refreshed.token, refreshed.refreshToken)
                    onAuthRefreshed(refreshed)
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${refreshed.token}")
                        .build()
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    private fun responseChainLength(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
