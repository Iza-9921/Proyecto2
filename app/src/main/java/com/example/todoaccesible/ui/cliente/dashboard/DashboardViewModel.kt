package com.example.todoaccesible.ui.cliente.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.data.repository.NotificationRepository
import com.example.todoaccesible.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    diagnosticRepository: DiagnosticRepository,
    notificationRepository: NotificationRepository,
    userRepository: UserRepository,
    clienteId: Long
) : ViewModel() {

    val diagnostics: StateFlow<List<DiagnosticEntity>> = diagnosticRepository
        .observeForCliente(clienteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotifications: StateFlow<Int> = notificationRepository
        .observeUnreadCount(clienteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * Cupo de diagnósticos que el administrador le asignó a este cliente;
     * `null` = ilimitados. Solo informativo. El valor inicial (antes de que
     * llegue el primer dato real) es `0`, no `null`: mientras carga, más vale
     * bloquear por defecto que dejar pasar como si fuera ilimitado.
     */
    val diagnosticosDisponibles: StateFlow<Int?> = userRepository.observeById(clienteId)
        .map { user -> if (user == null) 0 else user.diagnosticosDisponibles }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}
