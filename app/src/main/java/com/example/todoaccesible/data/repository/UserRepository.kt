package com.example.todoaccesible.data.repository

import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.model.Role
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeAll(): Flow<List<UserEntity>>
    fun observeById(userId: Long): Flow<UserEntity?>
    suspend fun create(nombre: String, email: String, password: String, rol: Role): Result<Long>
    suspend fun updateRole(userId: Long, rol: Role)
    suspend fun updateNombre(userId: Long, nombre: String)

    /** RF-03: activa/desactiva la licencia de un usuario; con licencia inactiva no puede iniciar sesión. */
    suspend fun setLicenseActive(userId: Long, active: Boolean)

    suspend fun delete(userId: Long)

    /** Asigna manualmente el cupo de diagnósticos de un cliente; `null` = ilimitados. */
    suspend fun setDiagnosticosDisponibles(userId: Long, cantidad: Int?)

    /** Descuenta un diagnóstico disponible tras un envío exitoso; no hace nada si ya es 0 o ilimitado. */
    suspend fun decrementDiagnosticoDisponible(userId: Long)

    /** Asigna el cuestionario (tipo de inmueble) que este cliente debe responder; lo hace el admin al activar la cuenta. */
    suspend fun assignCuestionario(userId: Long, tipo: String)
}
