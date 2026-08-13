package com.example.todoaccesible.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EmpresaInfoDto(
    val nombre: String? = null,
    val cliente: String? = null,
    val direccion: String? = null,
    val ciudad: String? = null,
    val estado: String? = null,
    val tipo: String? = null,
    val fecha: String? = null,
    val responsable: String? = null,
    val telefono: String? = null,
    val logo: String? = null
)

data class LoginRequest(val email: String, val password: String)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val empresa: EmpresaInfoDto? = null
)

/** Único campo del body de refresh; el backend lo espera en snake_case. */
data class RefreshRequest(@SerializedName("refresh_token") val refreshToken: String)

data class AuthUserDto(
    val id: Long,
    val name: String,
    val email: String,
    /** Crudo del backend: "admin" | "cliente". Se traduce a mano en el mapper, sin adapter automático de Gson. */
    val role: String,
    val empresa: EmpresaInfoDto? = null,
    val activo: Boolean,
    val cuestionarioAsignado: String? = null,
    val limiteCuestionarios: Int = 0
)

data class AuthResponseDto(
    val token: String,
    val refreshToken: String,
    val user: AuthUserDto
)

data class HeartbeatResponseDto(
    val ok: Boolean,
    val token: String,
    @SerializedName("expires_in") val expiresIn: Int,
    val message: String? = null
)

/** Shape genérico de error del backend: `{ error: string, details?: unknown }`. */
data class ApiErrorBody(
    val error: String? = null,
    val details: Any? = null
)
