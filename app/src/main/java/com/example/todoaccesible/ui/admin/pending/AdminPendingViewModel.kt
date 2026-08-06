package com.example.todoaccesible.ui.admin.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.core.designsystem.ToastController
import com.example.todoaccesible.core.designsystem.ToastTipo
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
import kotlinx.coroutines.launch

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

/** Estado destino al soltar una tarjeta sobre esta columna (RECHAZADO no tiene columna propia, se resuelve a INFO_REQUERIDA). */
fun KanbanColumn.toDiagnosticStatus(): DiagnosticStatus = when (this) {
    KanbanColumn.PENDIENTES -> DiagnosticStatus.PENDIENTE
    KanbanColumn.EN_REVISION -> DiagnosticStatus.EN_REVISION
    KanbanColumn.INFO_REQUERIDA -> DiagnosticStatus.INFO_REQUERIDA
    KanbanColumn.VALIDADOS -> DiagnosticStatus.VALIDADO
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
    private val diagnosticRepository: DiagnosticRepository,
    userRepository: UserRepository,
    private val reviewerId: Long,
    private val toastController: ToastController
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

    /** Arrastrar una tarjeta a otra columna del Kanban cambia su estado, igual que el drag-and-drop de `KanbanBoard.jsx`. */
    fun moveToColumn(diagnosticId: Long, column: KanbanColumn) {
        val nuevoEstado = column.toDiagnosticStatus()
        viewModelScope.launch {
            diagnosticRepository.updateStatus(diagnosticId, nuevoEstado, reviewerId)
            toastController.show("Movido a \"${column.label}\"", ToastTipo.INFO)
        }
    }
}
