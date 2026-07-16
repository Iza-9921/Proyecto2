package com.example.todoaccesible.ui.cliente.diagnostic.new

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.repository.DiagnosticRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProjectInfoUiState(
    val diagnosticId: Long? = null,
    val projectName: String = "",
    val ubicacion: String = "",
    val responsable: String = "",
    val revision: String = "1",
    val loading: Boolean = true
)

class ProjectInfoViewModel(
    private val diagnosticRepository: DiagnosticRepository,
    private val clienteId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectInfoUiState())
    val uiState: StateFlow<ProjectInfoUiState> = _uiState

    init {
        viewModelScope.launch {
            val draft = diagnosticRepository.getOrCreateDraft(clienteId)
            _uiState.value = ProjectInfoUiState(
                diagnosticId = draft.id,
                projectName = draft.projectName,
                ubicacion = draft.ubicacion,
                responsable = draft.responsable,
                revision = draft.revision,
                loading = false
            )
        }
    }

    fun onProjectNameChange(value: String) { _uiState.value = _uiState.value.copy(projectName = value) }
    fun onUbicacionChange(value: String) { _uiState.value = _uiState.value.copy(ubicacion = value) }
    fun onResponsableChange(value: String) { _uiState.value = _uiState.value.copy(responsable = value) }
    fun onRevisionChange(value: String) { _uiState.value = _uiState.value.copy(revision = value) }

    fun continueToQuestionnaire(onReady: (Long) -> Unit) {
        val state = _uiState.value
        val id = state.diagnosticId ?: return
        viewModelScope.launch {
            diagnosticRepository.updateProjectInfo(id, state.projectName, state.ubicacion, state.responsable, state.revision)
            onReady(id)
        }
    }
}
