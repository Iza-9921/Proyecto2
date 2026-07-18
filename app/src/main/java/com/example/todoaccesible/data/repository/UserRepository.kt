package com.example.todoaccesible.data.repository

import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.model.Role
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeAll(): Flow<List<UserEntity>>
    suspend fun create(nombre: String, email: String, password: String, rol: Role): Result<Long>
    suspend fun updateRole(userId: Long, rol: Role)
    suspend fun updateNombre(userId: Long, nombre: String)

    /** RF-03: activa/desactiva la licencia de un usuario; con licencia inactiva no puede iniciar sesión. */
    suspend fun setLicenseActive(userId: Long, active: Boolean)

    suspend fun delete(userId: Long)
}
