package com.example.todoaccesible.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.todoaccesible.data.local.entities.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert
    suspend fun insert(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET leido = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("SELECT * FROM notifications WHERE destinatarioId = :userId ORDER BY fecha DESC")
    fun observeForUser(userId: Long): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE destinatarioId = :userId AND leido = 0")
    fun observeUnreadCount(userId: Long): Flow<Int>
}
