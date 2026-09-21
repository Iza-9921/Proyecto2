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
import retrofit2.HttpException
import java.io.IOException

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
                } catch (e: HttpException) {
                    // Solo un 401/403 del propio /auth/refresh confirma que el refresh token ya no
                    // sirve: ahí sí dejamos que el 401 original se propague para forzar el logout.
                    if (e.code() == 401 || e.code() == 403) {
                        null
                    } else {
                        throw IOException("Fallo inesperado al refrescar el token", e)
                    }
                } catch (e: Exception) {
                    // Cualquier otra falla (sin conexión, timeout, respuesta malformada, etc.) es
                    // transitoria/inesperada, NO evidencia de que la sesión sea inválida: se envuelve
                    // como IOException para que se reporte como error de red en vez de forzar logout
                    // (ApiErrorMapper solo cierra sesión ante ApiError.Unauthorized).
                    throw IOException("Fallo inesperado al refrescar el token", e)
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
