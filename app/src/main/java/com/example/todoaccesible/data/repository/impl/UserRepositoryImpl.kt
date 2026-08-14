package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.core.designsystem.ToastController
import com.example.todoaccesible.core.designsystem.ToastTipo
import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.model.Role
import com.example.todoaccesible.data.preferences.SessionManager
import com.example.todoaccesible.data.remote.AdminApiService
import com.example.todoaccesible.data.remote.ApiErrorMapper
import com.example.todoaccesible.data.remote.dto.ActivoRequest
import com.example.todoaccesible.data.remote.dto.CuestionarioAsignadoRequest
import com.example.todoaccesible.data.remote.mapper.toEntity
import com.example.todoaccesible.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.math.abs

/**
 * `GET /admin/usuarios` es admin-only: un cliente no puede llamarlo. Para
 * que las pantallas de CLIENTE (`ProjectInfoViewModel`, `DashboardViewModel`)
 * puedan leer su propio `cuestionarioAsignado`/`diagnosticosDisponibles` sin
 * ese endpoint, [observeById] resuelve el caso "me estoy pidiendo a mí
 * mismo" con la caché de [AuthRepositoryImpl] (poblada por login/register/
 * refresh) en vez de llamar al endpoint admin. Para cualquier otro id, sí
 * llama a `GET /admin/usuarios` (falla en silencio -devuelve null/lista
 * vacía- si quien pregunta no es admin).
 */
class UserRepositoryImpl(
    private val adminApi: AdminApiService,
    private val sessionManager: SessionManager,
    private val selfUserFlow: StateFlow<UserEntity?>,
    private val toastController: ToastController
) : UserRepository {

    private val _adminUsers = MutableStateFlow<List<UserEntity>>(emptyList())

    private suspend fun refreshAdminUsers() {
        try {
            _adminUsers.value = adminApi.listarUsuarios().map { it.toEntity() }
        } catch (_: Exception) {
            // No es admin, o sin conexión: se deja la última lista conocida.
        }
    }

    override fun observeAll(): Flow<List<UserEntity>> = _adminUsers.onStart { refreshAdminUsers() }

    /**
     * OJO: `combine` se suscribe a TODOS sus flows de entrada sin importar
     * cuál termine usándose, así que combinar directamente con [observeAll]
     * disparaba `refreshAdminUsers()` (→ `GET /admin/usuarios`, 403 para un
     * cliente) incluso cuando el resultado iba a ser el atajo "self" y ese
     * valor se descartaba. `flatMapLatest` evita suscribirse a [observeAll]
     * salvo que de verdad se necesite (id distinto al propio).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeById(userId: Long): Flow<UserEntity?> =
        combine(sessionManager.session, selfUserFlow) { session, self -> session to self }
            .flatMapLatest { (session, self) ->
                if (session != null && session.userId == userId && self != null) {
                    flowOf(self)
                } else {
                    observeAll().map { admins -> admins.find { it.id == userId } }
                }
            }

    /**
     * No existe endpoint para crear usuarios desde el panel admin: el alta
     * real es el registro público (que siempre crea rol CLIENTE) más la
     * activación del admin. `UserManagementScreen` ya no ofrece este botón
     * (ver Fase 6); esta implementación solo evita dejar el método roto si
     * algo más lo llegara a invocar.
     */
    override suspend fun create(nombre: String, email: String, password: String, rol: Role): Result<Long> =
        Result.failure(
            UnsupportedOperationException(
                "No se pueden crear cuentas desde el panel admin: el cliente debe registrarse y luego activarse desde aquí."
            )
        )

    /** No hay endpoint para cambiar el rol: se fija en el registro según ADMIN_EMAILS del backend. No-op intencional. */
    override suspend fun updateRole(userId: Long, rol: Role) = Unit

    /** No hay endpoint para renombrar un usuario desde el panel admin. No-op intencional. */
    override suspend fun updateNombre(userId: Long, nombre: String) = Unit

    override suspend fun setLicenseActive(userId: Long, active: Boolean) {
        runApi { adminApi.setActivo(userId, ActivoRequest(active)) }
    }

    override suspend fun delete(userId: Long) {
        runApi { adminApi.eliminarUsuario(userId) }
    }

    /**
     * El backend solo expone +1/-1 (`POST`/`DELETE .../limite-cuestionarios`),
     * no "fijar a N". La UI (`UserManagementScreen`) solo mueve el valor de a
     * uno por toque, así que en la práctica [delta] casi siempre es ±1; por
     * robustez se manda como una serie de llamadas +1/-1 desde el último
     * valor conocido.
     */
    override suspend fun setDiagnosticosDisponibles(userId: Long, cantidad: Int?) {
        if (cantidad == null) return // el backend no tiene concepto de "ilimitado"
        val current = _adminUsers.value.find { it.id == userId }?.diagnosticosDisponibles ?: 0
        val delta = cantidad - current
        if (delta == 0) return
        runApi {
            repeat(abs(delta)) {
                if (delta > 0) adminApi.incrementarLimite(userId) else adminApi.decrementarLimite(userId)
            }
        }
    }

    override suspend fun decrementDiagnosticoDisponible(userId: Long) {
        runApi { adminApi.decrementarLimite(userId) }
    }

    override suspend fun assignCuestionario(userId: Long, tipo: String) {
        runApi { adminApi.setCuestionarioAsignado(userId, CuestionarioAsignadoRequest(tipo)) }
    }

    private suspend fun runApi(block: suspend () -> Unit) {
        try {
            block()
            refreshAdminUsers()
        } catch (e: Exception) {
            val mapped = ApiErrorMapper.handle(e, sessionManager)
            toastController.show(mapped.message, ToastTipo.ERROR)
        }
    }
}
