package com.example.todoaccesible.ui.cliente.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.local.entities.NotificationEntity
import com.example.todoaccesible.data.repository.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository,
    private val userId: Long
) : ViewModel() {

    val notifications: StateFlow<List<NotificationEntity>> = notificationRepository
        .observeForUser(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markRead(id: Long) {
        viewModelScope.launch { notificationRepository.markRead(id) }
    }
}
