package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.core.util.PasswordHasher
import com.example.todoaccesible.data.local.dao.UserDao
import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.model.Role
import com.example.todoaccesible.data.repository.UserRepository

class UserRepositoryImpl(private val userDao: UserDao) : UserRepository {

    companion object {
        const val DEFAULT_ADMIN_EMAIL = "admin@todoaccesible.mx"
        const val DEFAULT_ADMIN_PASSWORD = "TodoAccesible2026"
    }

    override fun observeAll() = userDao.observeAll()

    override suspend fun create(nombre: String, email: String, password: String, rol: Role): Result<Long> {
        val normalizedEmail = email.trim().lowercase()
        if (userDao.findByEmail(normalizedEmail) != null) {
            return Result.failure(IllegalStateException("Ya existe una cuenta con ese correo"))
        }
        val id = userDao.insert(
            UserEntity(
                email = normalizedEmail,
                passwordHash = PasswordHasher.hash(password),
                nombre = nombre.trim(),
                rol = rol
            )
        )
        return Result.success(id)
    }

    override suspend fun updateRole(userId: Long, rol: Role) {
        userDao.findById(userId)?.let { userDao.update(it.copy(rol = rol)) }
    }

    override suspend fun updateNombre(userId: Long, nombre: String) {
        userDao.findById(userId)?.let { userDao.update(it.copy(nombre = nombre)) }
    }

    override suspend fun delete(userId: Long) {
        userDao.delete(userId)
    }

    override suspend fun ensureDefaultAdminSeeded() {
        if (userDao.findByEmail(DEFAULT_ADMIN_EMAIL) == null) {
            userDao.insert(
                UserEntity(
                    email = DEFAULT_ADMIN_EMAIL,
                    passwordHash = PasswordHasher.hash(DEFAULT_ADMIN_PASSWORD),
                    nombre = "Administrador",
                    rol = Role.ADMIN
                )
            )
        }
    }
}
