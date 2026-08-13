package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.preferences.SessionManager
import com.example.todoaccesible.data.preferences.UserSession
import com.example.todoaccesible.data.remote.ApiError
import com.example.todoaccesible.data.remote.ApiErrorMapper
import com.example.todoaccesible.data.remote.AuthApiService
import com.example.todoaccesible.data.remote.dto.AuthResponseDto
import com.example.todoaccesible.data.remote.dto.LoginRequest
import com.example.todoaccesible.data.remote.dto.RegisterRequest
import com.example.todoaccesible.data.remote.mapper.toEntity
import com.example.todoaccesible.data.repository.AuthRepository
import com.example.todoaccesible.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Auth real contra el backend. No hay endpoint "me": el único lugar donde el
 * servidor manda la info completa del usuario es la respuesta de
 * login/register/refresh, así que se cachea aquí ([cachedUserFlow]) para que
 * [currentUser] y [UserRepositoryImpl] (para el propio cliente autenticado)
 * tengan algo de donde leer. [onAuthResponse] también la actualiza
 * [com.example.todoaccesible.data.remote.TokenAuthenticator] en cada refresh
 * silencioso (conectado desde `AppContainer`), así que en la práctica se
 * refresca cada vez que expira el access token corto (15 min).
 */
class AuthRepositoryImpl(
    private val authApiPlain: AuthApiService,
    private val sessionManager: SessionManager
) : AuthRepository {

    override val session = sessionManager.session

    private val _cachedUser = MutableStateFlow<UserEntity?>(null)
    val cachedUserFlow: StateFlow<UserEntity?> = _cachedUser

    fun onAuthResponse(response: AuthResponseDto) {
        _cachedUser.value = response.user.toEntity()
    }

    override suspend fun register(nombre: String, email: String, password: String): AuthResult {
        return try {
            val response = authApiPlain.register(
                RegisterRequest(name = nombre.trim(), email = email.trim().lowercase(), password = password)
            )
            onAuthResponse(response)
            val entity = response.user.toEntity()
            sessionManager.startSession(entity.id, entity.rol, response.token, response.refreshToken)
            AuthResult.Success(UserSession(entity.id, entity.rol))
        } catch (e: Exception) {
            AuthResult.Error(ApiErrorMapper.from(e).message)
        }
    }

    override suspend fun login(email: String, password: String): AuthResult {
        return try {
            val response = authApiPlain.login(LoginRequest(email = email.trim().lowercase(), password = password))
            onAuthResponse(response)
            val entity = response.user.toEntity()
            sessionManager.startSession(entity.id, entity.rol, response.token, response.refreshToken)
            AuthResult.Success(UserSession(entity.id, entity.rol))
        } catch (e: Exception) {
            val mapped = ApiErrorMapper.from(e)
            val message = when (mapped) {
                // 401 en login: el backend usa el mismo mensaje para email inexistente y password incorrecta.
                is ApiError.Unauthorized -> "Correo o contraseña incorrectos"
                else -> mapped.message
            }
            AuthResult.Error(message)
        }
    }

    override suspend fun logout() {
        sessionManager.endSession()
        _cachedUser.value = null
    }

    override suspend fun currentUser(): UserEntity? = _cachedUser.value
}
