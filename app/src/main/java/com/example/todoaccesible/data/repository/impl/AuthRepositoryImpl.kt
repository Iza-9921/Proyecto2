package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.core.util.PasswordHasher
import com.example.todoaccesible.data.local.dao.UserDao
import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.model.Role
import com.example.todoaccesible.data.preferences.SessionManager
import com.example.todoaccesible.data.preferences.UserSession
import com.example.todoaccesible.data.repository.AuthRepository
import com.example.todoaccesible.data.repository.AuthResult
import kotlinx.coroutines.flow.firstOrNull

class AuthRepositoryImpl(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) : AuthRepository {

    override val session = sessionManager.session

    override suspend fun register(nombre: String, email: String, password: String): AuthResult {
        val normalizedEmail = email.trim().lowercase()
        if (userDao.findByEmail(normalizedEmail) != null) {
            return AuthResult.Error("Ya existe una cuenta con ese correo")
        }
        val user = UserEntity(
            email = normalizedEmail,
            passwordHash = PasswordHasher.hash(password),
            nombre = nombre.trim(),
            rol = Role.CLIENTE
        )
        val id = userDao.insert(user)
        sessionManager.startSession(id, Role.CLIENTE)
        return AuthResult.Success(UserSession(id, Role.CLIENTE))
    }

    override suspend fun login(email: String, password: String): AuthResult {
        val user = userDao.findByEmail(email.trim().lowercase())
            ?: return AuthResult.Error("Correo o contraseña incorrectos")
        if (!PasswordHasher.matches(password, user.passwordHash)) {
            return AuthResult.Error("Correo o contraseña incorrectos")
        }
        sessionManager.startSession(user.id, user.rol)
        return AuthResult.Success(UserSession(user.id, user.rol))
    }

    override suspend fun logout() {
        sessionManager.endSession()
    }

    override suspend fun currentUser(): UserEntity? {
        val active = session.firstOrNull() ?: return null
        return userDao.findById(active.userId)
    }
}
