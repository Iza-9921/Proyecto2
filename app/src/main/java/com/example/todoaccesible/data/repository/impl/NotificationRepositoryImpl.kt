package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.core.designsystem.ToastController
import com.example.todoaccesible.core.designsystem.ToastTipo
import com.example.todoaccesible.data.local.entities.NotificationEntity
import com.example.todoaccesible.data.preferences.SessionManager
import com.example.todoaccesible.data.remote.ApiErrorMapper
import com.example.todoaccesible.data.remote.NotificacionApiService
import com.example.todoaccesible.data.remote.mapper.toEntity
import com.example.todoaccesible.data.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

/**
 * `GET /notificaciones` sembrado al primer `observeForUser` y actualizado en
 * vivo por [com.example.todoaccesible.data.remote.SocketManager] (evento
 * `notificacion_nueva`) vía [onSocketNotification] -- sin polling.
 */
class NotificationRepositoryImpl(
    private val notificacionApi: NotificacionApiService,
    private val sessionManager: SessionManager,
    private val toastController: ToastController
) : NotificationRepository {

    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    private var loadedForUser: Long? = null

    /**
     * El backend no expone un endpoint para crear notificaciones "a mano":
     * las genera él mismo como efecto secundario de aprobar / rechazar /
     * solicitar-info / finalizar / guardar-respuestas. No-op intencional
     * (ver Fase 5/9 del reporte: la mayoría de los llamados a `notify()` que
     * había en el repo viejo ya se borraron por ser código muerto).
     */
    override suspend fun notify(diagnosticId: Long?, destinatarioId: Long, tipo: String, mensaje: String) = Unit

    override fun observeForUser(userId: Long): Flow<List<NotificationEntity>> =
        _notifications
            .onStart { if (loadedForUser != userId) { loadedForUser = userId; runCatching { refresh(userId) } } }
            .map { list -> list.filter { it.destinatarioId == userId }.sortedByDescending { it.fecha } }

    override fun observeUnreadCount(userId: Long): Flow<Int> =
        observeForUser(userId).map { list -> list.count { !it.leido } }

    override suspend fun markRead(id: Long) {
        _notifications.update { list -> list.map { if (it.id == id) it.copy(leido = true) else it } }
        try {
            notificacionApi.marcarLeida(id)
        } catch (e: Exception) {
            val mapped = ApiErrorMapper.handle(e, sessionManager)
            toastController.show(mapped.message, ToastTipo.ERROR)
        }
    }

    private suspend fun refresh(userId: Long) {
        val response = notificacionApi.listar(limit = 100)
        _notifications.value = response.notificaciones.map { it.toEntity(userId) }
    }

    /** Conectado desde `AppContainer` al evento `notificacion_nueva` de [com.example.todoaccesible.data.remote.SocketManager]. */
    fun onSocketNotification(entity: NotificationEntity) {
        _notifications.update { list -> listOf(entity) + list.filterNot { it.id == entity.id } }
    }
}
