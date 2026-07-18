package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.core.util.PasswordHasher
import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.local.memory.InMemoryTable
import com.example.todoaccesible.data.model.Role
import com.example.todoaccesible.data.repository.UserRepository

class UserRepositoryImpl(private val users: InMemoryTable<UserEntity>) : UserRepository {

    companion object {
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
                id = 1,
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
            )
        )
    }

    override fun observeAll() = users.flow

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
}
