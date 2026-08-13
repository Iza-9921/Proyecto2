package com.example.todoaccesible.data.repository

import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.preferences.UserSession
import kotlinx.coroutines.flow.Flow

sealed class AuthResult {
    data class Success(val session: UserSession) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

interface AuthRepository {
    val session: Flow<UserSession?>

    /** El registro público siempre crea un usuario con rol CLIENTE. */
    suspend fun register(nombre: String, email: String, password: String): AuthResult

    suspend fun login(email: String, password: String): AuthResult

    suspend fun logout()

    suspend fun currentUser(): UserEntity?
}
