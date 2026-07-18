package com.example.todoaccesible.data.repository

import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.model.Role
import com.example.todoaccesible.data.preferences.UserSession
import kotlinx.coroutines.flow.Flow

sealed class AuthResult {
    data class Success(val session: UserSession) : AuthResult()
    data class Error(val message: String) : AuthResult()

    /** RF-18: ya existe una sesión activa con esta cuenta; hay que forzar el cierre para continuar. */
    data class SessionConflict(val userId: Long, val rol: Role) : AuthResult()
}

interface AuthRepository {
    val session: Flow<UserSession?>

    /** El registro público siempre crea un usuario con rol CLIENTE. */
    suspend fun register(nombre: String, email: String, password: String): AuthResult

    /** [force] ignora un conflicto de sesión activa y la reemplaza (RF-18). */
    suspend fun login(email: String, password: String, force: Boolean = false): AuthResult

    suspend fun logout()

    suspend fun currentUser(): UserEntity?
}
