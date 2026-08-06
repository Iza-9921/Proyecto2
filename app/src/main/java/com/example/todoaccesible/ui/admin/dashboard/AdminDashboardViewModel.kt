package com.example.todoaccesible.ui.admin.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.model.DiagnosticStatus
import com.example.todoaccesible.data.model.Nivel
import com.example.todoaccesible.data.preferences.PanelCollapseStore
import com.example.todoaccesible.data.repository.DiagnosticRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdminDashboardUiState(
    val total: Int = 0,
    val porEstado: Map<DiagnosticStatus, Int> = emptyMap(),
    val porNivel: Map<Nivel, Int> = emptyMap(),
    /** Diagnósticos evaluados (no borrador) agrupados por nivel alcanzado, para el leaderboard. */
    val porNivelGrouped: Map<Nivel, List<DiagnosticEntity>> = emptyMap(),
    val collapsedNiveles: Set<String> = emptySet()
)

class AdminDashboardViewModel(
    diagnosticRepository: DiagnosticRepository,
    private val panelCollapseStore: PanelCollapseStore
) : ViewModel() {

    val uiState: StateFlow<AdminDashboardUiState> = combine(
        diagnosticRepository.observeAllSubmitted(),
        panelCollapseStore.collapsed
    ) { diagnostics, collapsed ->
        AdminDashboardUiState(
            total = diagnostics.size,
            porEstado = diagnostics.groupingBy { it.estado }.eachCount(),
            porNivel = diagnostics.mapNotNull { it.nivel }.groupingBy { it }.eachCount(),
            porNivelGrouped = Nivel.entries.associateWith { nivel -> diagnostics.filter { it.nivel == nivel } },
            collapsedNiveles = collapsed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminDashboardUiState())

    fun toggleNivelCollapse(nivel: Nivel) {
        viewModelScope.launch { panelCollapseStore.toggle(nivel.name) }
    }
}
