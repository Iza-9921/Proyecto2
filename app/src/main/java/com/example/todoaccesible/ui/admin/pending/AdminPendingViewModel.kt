package com.example.todoaccesible.ui.admin.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.model.DiagnosticStatus
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Las 4 columnas del Kanban de admin. RECHAZADO se agrupa dentro de INFO_REQUERIDA. */
enum class KanbanColumn(val label: String) {
    PENDIENTES("Pendientes"),
    EN_REVISION("En revisión"),
    INFO_REQUERIDA("Info requerida"),
    VALIDADOS("Validados")
}

fun DiagnosticStatus.kanbanColumn(): KanbanColumn = when (this) {
    DiagnosticStatus.PENDIENTE -> KanbanColumn.PENDIENTES
    DiagnosticStatus.EN_REVISION -> KanbanColumn.EN_REVISION
    DiagnosticStatus.INFO_REQUERIDA, DiagnosticStatus.RECHAZADO -> KanbanColumn.INFO_REQUERIDA
    DiagnosticStatus.VALIDADO -> KanbanColumn.VALIDADOS
    DiagnosticStatus.BORRADOR -> KanbanColumn.PENDIENTES
}

data class AdminPendingUiState(
    val diagnostics: List<DiagnosticEntity> = emptyList(),
    val usersById: Map<Long, UserEntity> = emptyMap(),
    val filterEstado: DiagnosticStatus? = null,
    val query: String = ""
) {
    val filtered: List<DiagnosticEntity> get() = diagnostics.filter { d ->
        (filterEstado == null || d.estado == filterEstado) &&
            (query.isBlank() || d.projectName.contains(query, ignoreCase = true) ||
                usersById[d.clienteId]?.nombre?.contains(query, ignoreCase = true) == true)
    }
}

class AdminPendingViewModel(
    diagnosticRepository: DiagnosticRepository,
    userRepository: UserRepository
) : ViewModel() {

    private val _filterEstado = MutableStateFlow<DiagnosticStatus?>(null)
    private val _query = MutableStateFlow("")

    val uiState: StateFlow<AdminPendingUiState> = combine(
        diagnosticRepository.observeAllSubmitted(),
        userRepository.observeAll(),
        _filterEstado,
        _query
    ) { diagnostics, users, filterEstado, query ->
        AdminPendingUiState(
            diagnostics = diagnostics,
            usersById = users.associateBy { it.id },
            filterEstado = filterEstado,
            query = query
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminPendingUiState())

    fun setFilterEstado(estado: DiagnosticStatus?) { _filterEstado.value = estado }
    fun setQuery(value: String) { _query.value = value }
}
