package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.data.local.entities.NotificationEntity
import com.example.todoaccesible.data.local.memory.InMemoryTable
import com.example.todoaccesible.data.repository.NotificationRepository
import kotlinx.coroutines.flow.map

class NotificationRepositoryImpl(
    private val notifications: InMemoryTable<NotificationEntity> = InMemoryTable()
) : NotificationRepository {

    override suspend fun notify(diagnosticId: Long, destinatarioId: Long, tipo: String, mensaje: String) {
        val id = notifications.nextId()
        notifications.mutate {
            it + NotificationEntity(
                id = id,
                diagnosticId = diagnosticId,
                destinatarioId = destinatarioId,
                tipo = tipo,
                mensaje = mensaje,
                fecha = System.currentTimeMillis()
            )
        }
    }

    override fun observeForUser(userId: Long) =
        notifications.flow.map { list -> list.filter { it.destinatarioId == userId }.sortedByDescending { it.fecha } }

    override fun observeUnreadCount(userId: Long) =
        notifications.flow.map { list -> list.count { it.destinatarioId == userId && !it.leido } }

    override suspend fun markRead(id: Long) {
        notifications.mutate { list -> list.map { if (it.id == id) it.copy(leido = true) else it } }
    }
}
