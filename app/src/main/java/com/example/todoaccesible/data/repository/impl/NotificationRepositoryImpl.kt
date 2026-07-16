package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.data.local.dao.NotificationDao
import com.example.todoaccesible.data.local.entities.NotificationEntity
import com.example.todoaccesible.data.repository.NotificationRepository

class NotificationRepositoryImpl(
    private val dao: NotificationDao
) : NotificationRepository {

    override suspend fun notify(diagnosticId: Long, destinatarioId: Long, tipo: String, mensaje: String) {
        dao.insert(
            NotificationEntity(
                diagnosticId = diagnosticId,
                destinatarioId = destinatarioId,
                tipo = tipo,
                mensaje = mensaje,
                fecha = System.currentTimeMillis()
            )
        )
    }

    override fun observeForUser(userId: Long) = dao.observeForUser(userId)

    override fun observeUnreadCount(userId: Long) = dao.observeUnreadCount(userId)

    override suspend fun markRead(id: Long) = dao.markRead(id)
}
