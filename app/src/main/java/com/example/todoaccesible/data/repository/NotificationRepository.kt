package com.example.todoaccesible.data.repository

import com.example.todoaccesible.data.local.entities.NotificationEntity
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    suspend fun notify(diagnosticId: Long?, destinatarioId: Long, tipo: String, mensaje: String)
    fun observeForUser(userId: Long): Flow<List<NotificationEntity>>
    fun observeUnreadCount(userId: Long): Flow<Int>
    suspend fun markRead(id: Long)
}
