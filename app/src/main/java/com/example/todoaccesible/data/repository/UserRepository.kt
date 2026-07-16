package com.example.todoaccesible.data.repository

import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.model.Role
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeAll(): Flow<List<UserEntity>>
    suspend fun create(nombre: String, email: String, password: String, rol: Role): Result<Long>
    suspend fun updateRole(userId: Long, rol: Role)
    suspend fun updateNombre(userId: Long, nombre: String)
    suspend fun delete(userId: Long)

    /** Siembra `admin@todoaccesible.mx` la primera vez que se abre la app, ya
     * que el registro público solo crea rol CLIENTE y no hay backend que
     * provea una cuenta admin. */
    suspend fun ensureDefaultAdminSeeded()
}
