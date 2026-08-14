package com.example.todoaccesible.data.remote

import com.example.todoaccesible.data.preferences.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Agrega `Authorization: Bearer <token>` a toda request salvo las de
 * auth público (login/register/refresh/recuperación). En la práctica estas
 * ya se llaman con un cliente separado sin este interceptor (ver
 * [NetworkModule]); esta lista es una segunda capa de seguridad por si algún
 * llamado futuro pasa por aquí por error.
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    private val publicPaths = listOf(
        "auth/login", "auth/register", "auth/refresh",
        "auth/recuperar", "auth/verificar-codigo", "auth/nueva-contrasena"
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath
        if (publicPaths.any { path.contains(it) }) {
            return chain.proceed(original)
        }
        // El interceptor corre en el hilo de OkHttp (no en el principal), por
        // lo que bloquear aquí con runBlocking es aceptable.
        val token = runBlocking { sessionManager.tokens.first() }?.accessToken
        val request = if (token != null) {
            original.newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            original
        }
        return chain.proceed(request)
    }
}
