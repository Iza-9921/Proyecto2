package com.example.todoaccesible.data.remote

import com.example.todoaccesible.data.preferences.SessionManager
import com.example.todoaccesible.data.remote.dto.ApiErrorBody
import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

/** Clasificación de errores de red para mostrar mensajes útiles en la UI. */
sealed class ApiError(val message: String) {
    /** 401: sesión inválida/expirada de verdad (ya se intentó refrescar y falló) — hay que forzar logout. */
    class Unauthorized(message: String) : ApiError(message)

    /** 403 al crear un proyecto/diagnóstico sin licencia activa. */
    class LicenciaVencida(message: String) : ApiError(message)

    /** 403 al hacer login con una cuenta aún no activada por el administrador. */
    class CuentaInactiva(message: String) : ApiError(message)

    /** Sin conexión, timeout, DNS, etc. */
    class NetworkError(message: String) : ApiError(message)

    /** 400 con detalles de validación (Zod u otro). */
    class ValidationError(message: String, val details: Any? = null) : ApiError(message)

    class Generic(message: String) : ApiError(message)
}

object ApiErrorMapper {
    private val gson = Gson()

    fun from(throwable: Throwable): ApiError = when (throwable) {
        is HttpException -> fromHttp(throwable)
        is IOException -> ApiError.NetworkError("Sin conexión a internet. Verifica tu red e intenta de nuevo.")
        else -> ApiError.Generic(throwable.message ?: "Ocurrió un error inesperado.")
    }

    /** Además de clasificar, si es [ApiError.Unauthorized] cierra la sesión local (el token ya no sirve). */
    suspend fun handle(throwable: Throwable, sessionManager: SessionManager): ApiError {
        val mapped = from(throwable)
        if (mapped is ApiError.Unauthorized) {
            sessionManager.endSession()
        }
        return mapped
    }

    private fun fromHttp(e: HttpException): ApiError {
        val serverMessage = try {
            e.response()?.errorBody()?.string()
                ?.let { gson.fromJson(it, ApiErrorBody::class.java) }
                ?.error
        } catch (_: Exception) {
            null
        }
        return when (e.code()) {
            401 -> ApiError.Unauthorized(serverMessage ?: "Credenciales inválidas.")
            403 -> when {
                serverMessage?.contains("licencia", ignoreCase = true) == true ->
                    ApiError.LicenciaVencida(serverMessage)
                serverMessage?.contains("pendiente de activación", ignoreCase = true) == true ->
                    ApiError.CuentaInactiva(serverMessage)
                else -> ApiError.Generic(serverMessage ?: "No tienes permiso para hacer esto.")
            }
            400 -> ApiError.ValidationError(serverMessage ?: "Datos inválidos.")
            else -> ApiError.Generic(serverMessage ?: "Ocurrió un error (${e.code()}). Intenta de nuevo.")
        }
    }
}
