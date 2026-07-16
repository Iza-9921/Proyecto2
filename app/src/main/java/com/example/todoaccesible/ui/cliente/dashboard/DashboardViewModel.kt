package com.example.todoaccesible.ui.cliente.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.data.repository.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    diagnosticRepository: DiagnosticRepository,
    notificationRepository: NotificationRepository,
    clienteId: Long
) : ViewModel() {

    val diagnostics: StateFlow<List<DiagnosticEntity>> = diagnosticRepository
        .observeForCliente(clienteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotifications: StateFlow<Int> = notificationRepository
        .observeUnreadCount(clienteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}
