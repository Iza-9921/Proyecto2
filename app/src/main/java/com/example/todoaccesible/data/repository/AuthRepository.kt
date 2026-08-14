package com.example.todoaccesible.data.repository

import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.preferences.UserSession
import kotlinx.coroutines.flow.Flow

sealed class AuthResult {
    data class Success(val session: UserSession) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/** Resultado genérico para pasos del flujo de "olvidé mi contraseña" que solo devuelven un mensaje. */
sealed class PasswordResetResult {
    data class Success(val message: String) : PasswordResetResult()
    data class Error(val message: String) : PasswordResetResult()
}

/** Paso "verificar código": si es válido, el backend regresa un `reset_token` de un solo uso (10 min) para el paso final. */
sealed class VerifyResetCodeResult {
    data class Success(val resetToken: String) : VerifyResetCodeResult()
    data class Error(val message: String) : VerifyResetCodeResult()
}

interface AuthRepository {
    val session: Flow<UserSession?>

    /** El registro público siempre crea un usuario con rol CLIENTE. */
    suspend fun register(nombre: String, email: String, password: String): AuthResult

    suspend fun login(email: String, password: String): AuthResult

    suspend fun logout()

    suspend fun currentUser(): UserEntity?

    /** Paso 1 de "olvidé mi contraseña": pide al backend enviar un código de 6 dígitos al correo. */
    suspend fun requestPasswordReset(email: String): PasswordResetResult

    /** Paso 2: valida el código recibido por correo y obtiene el `reset_token` para fijar la nueva contraseña. */
    suspend fun verifyResetCode(email: String, codigo: String): VerifyResetCodeResult

    /** Paso 3: fija la nueva contraseña usando el `reset_token` obtenido en el paso anterior. */
    suspend fun setNewPassword(resetToken: String, nuevaContrasena: String): PasswordResetResult
}
