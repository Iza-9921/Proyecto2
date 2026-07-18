package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.core.util.PasswordHasher
import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.local.memory.InMemoryTable
import com.example.todoaccesible.data.model.Role
import com.example.todoaccesible.data.preferences.ActiveSessionRegistry
import com.example.todoaccesible.data.preferences.SessionManager
import com.example.todoaccesible.data.preferences.UserSession
import com.example.todoaccesible.data.repository.AuthRepository
import com.example.todoaccesible.data.repository.AuthResult
import kotlinx.coroutines.flow.firstOrNull

class AuthRepositoryImpl(
    private val users: InMemoryTable<UserEntity>,
    private val sessionManager: SessionManager,
    private val activeSessionRegistry: ActiveSessionRegistry
) : AuthRepository {

    override val session = sessionManager.session

    override suspend fun register(nombre: String, email: String, password: String): AuthResult {
        val normalizedEmail = email.trim().lowercase()
        if (users.snapshot.any { it.email == normalizedEmail }) {
            return AuthResult.Error("Ya existe una cuenta con ese correo")
        }
        val id = users.nextId()
        users.mutate {
            it + UserEntity(
                id = id,
                email = normalizedEmail,
                passwordHash = PasswordHasher.hash(password),
                nombre = nombre.trim(),
                rol = Role.CLIENTE
            )
        }
        sessionManager.startSession(id, Role.CLIENTE)
        activeSessionRegistry.markActive(id)
        return AuthResult.Success(UserSession(id, Role.CLIENTE))
    }

    override suspend fun login(email: String, password: String, force: Boolean): AuthResult {
        val user = users.snapshot.find { it.email == email.trim().lowercase() }
            ?: return AuthResult.Error("Correo o contraseña incorrectos")
        if (!PasswordHasher.matches(password, user.passwordHash)) {
            return AuthResult.Error("Correo o contraseña incorrectos")
        }
        if (!user.licenseActive) {
            return AuthResult.Error("Tu licencia está inactiva. Contacta al administrador.")
        }
        if (!force && activeSessionRegistry.isActive(user.id)) {
            return AuthResult.SessionConflict(user.id, user.rol)
        }
        sessionManager.startSession(user.id, user.rol)
        activeSessionRegistry.markActive(user.id)
        return AuthResult.Success(UserSession(user.id, user.rol))
    }

    override suspend fun logout() {
        session.firstOrNull()?.let { activeSessionRegistry.markInactive(it.userId) }
        sessionManager.endSession()
    }

    override suspend fun currentUser(): UserEntity? {
        val active = session.firstOrNull() ?: return null
        return users.snapshot.find { it.id == active.userId }
    }
}
