package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.core.util.PasswordHasher
import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.local.memory.InMemoryTable
import com.example.todoaccesible.data.model.Role
import com.example.todoaccesible.data.preferences.DiagnosticQuotaStore
import com.example.todoaccesible.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class UserRepositoryImpl(
    private val users: InMemoryTable<UserEntity>,
    private val quotaStore: DiagnosticQuotaStore
) : UserRepository {

    companion object {
        const val DEFAULT_ADMIN_ID = 1L
        const val DEFAULT_ADMIN_EMAIL = "admin@todoaccesible.mx"
        const val DEFAULT_ADMIN_PASSWORD = "TodoAccesible2026"

        const val DEMO_CLIENT_ID = 2L
        const val DEMO_CLIENT_EMAIL = "cliente@todoaccesible.mx"
        const val DEMO_CLIENT_PASSWORD = "ClienteDemo2026"

        /**
         * Cuentas por defecto: no hay backend que las provea, así que se siembran al
         * construir la app. La cuenta cliente demo existe para poder probar el flujo
         * completo (incluida la descarga del PDF) sin tener que registrar un usuario
         * ni contestar el cuestionario de 187 preguntas a mano; ver
         * [DiagnosticRepositoryImpl] para el diagnóstico ya completo que se le siembra.
         */
        fun defaultUsers(): List<UserEntity> = listOf(
            UserEntity(
                id = DEFAULT_ADMIN_ID,
                email = DEFAULT_ADMIN_EMAIL,
                passwordHash = PasswordHasher.hash(DEFAULT_ADMIN_PASSWORD),
                nombre = "Administrador",
                rol = Role.ADMIN,
                licenseActive = true
            ),
            UserEntity(
                id = DEMO_CLIENT_ID,
                email = DEMO_CLIENT_EMAIL,
                passwordHash = PasswordHasher.hash(DEMO_CLIENT_PASSWORD),
                nombre = "Cliente Demo",
                rol = Role.CLIENTE,
                licenseActive = true
                // diagnosticosDisponibles: sembrado por separado en DiagnosticQuotaStore (AppContainer),
                // que es la fuente real de este dato; ver seedIfAbsent().
            )
        )
    }

    /**
     * El cupo de diagnósticos ([UserEntity.diagnosticosDisponibles]) vive en
     * [DiagnosticQuotaStore] (DataStore), no en la tabla en memoria, para que
     * sobreviva a que se reinicie el proceso; aquí se combina con los demás
     * campos del usuario antes de exponerlo.
     */
    override fun observeAll(): Flow<List<UserEntity>> =
        combine(users.flow, quotaStore.preferences) { list, prefs ->
            list.map { it.copy(diagnosticosDisponibles = quotaStore.read(prefs, it.id)) }
        }

    override fun observeById(userId: Long): Flow<UserEntity?> =
        observeAll().map { list -> list.find { it.id == userId } }

    override suspend fun create(nombre: String, email: String, password: String, rol: Role): Result<Long> {
        val normalizedEmail = email.trim().lowercase()
        if (users.snapshot.any { it.email == normalizedEmail }) {
            return Result.failure(IllegalStateException("Ya existe una cuenta con ese correo"))
        }
        val id = users.nextId()
        users.mutate {
            it + UserEntity(
                id = id,
                email = normalizedEmail,
                passwordHash = PasswordHasher.hash(password),
                nombre = nombre.trim(),
                rol = rol
            )
        }
        return Result.success(id)
    }

    override suspend fun updateRole(userId: Long, rol: Role) {
        users.mutate { list -> list.map { if (it.id == userId) it.copy(rol = rol) else it } }
    }

    override suspend fun updateNombre(userId: Long, nombre: String) {
        users.mutate { list -> list.map { if (it.id == userId) it.copy(nombre = nombre) else it } }
    }

    override suspend fun setLicenseActive(userId: Long, active: Boolean) {
        users.mutate { list -> list.map { if (it.id == userId) it.copy(licenseActive = active) else it } }
    }

    override suspend fun delete(userId: Long) {
        users.mutate { list -> list.filterNot { it.id == userId } }
    }

    override suspend fun setDiagnosticosDisponibles(userId: Long, cantidad: Int?) {
        quotaStore.set(userId, cantidad)
    }

    override suspend fun decrementDiagnosticoDisponible(userId: Long) {
        quotaStore.decrement(userId)
    }

    override suspend fun assignCuestionario(userId: Long, tipo: String) {
        users.mutate { list -> list.map { if (it.id == userId) it.copy(cuestionarioAsignado = tipo) else it } }
    }
}
