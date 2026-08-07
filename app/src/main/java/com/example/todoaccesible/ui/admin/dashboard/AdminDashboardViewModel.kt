package com.example.todoaccesible.ui.admin.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.model.DiagnosticStatus
import com.example.todoaccesible.data.model.Nivel
import com.example.todoaccesible.data.repository.DiagnosticRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AdminDashboardUiState(
    val total: Int = 0,
    val porEstado: Map<DiagnosticStatus, Int> = emptyMap(),
    val porNivel: Map<Nivel, Int> = emptyMap()
)

class AdminDashboardViewModel(
    diagnosticRepository: DiagnosticRepository
) : ViewModel() {

    val uiState: StateFlow<AdminDashboardUiState> = diagnosticRepository.observeAllSubmitted()
        .map { diagnostics ->
            AdminDashboardUiState(
                total = diagnostics.size,
                porEstado = diagnostics.groupingBy { it.estado }.eachCount(),
                porNivel = diagnostics.mapNotNull { it.nivel }.groupingBy { it }.eachCount()
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminDashboardUiState())
}
