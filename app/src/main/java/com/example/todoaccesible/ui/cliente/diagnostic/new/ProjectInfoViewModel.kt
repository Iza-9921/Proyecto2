package com.example.todoaccesible.ui.cliente.diagnostic.new

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.local.seed.MexicoLocations
import com.example.todoaccesible.data.repository.DiagnosticRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProjectInfoUiState(
    val diagnosticId: Long? = null,
    val projectName: String = "",
    val clienteNombre: String = "",
    val telefono: String = "",
    val ubicacion: String = "",
    val entidadFederativa: String = "",
    val ciudad: String = "",
    val tipoInmueble: String = "",
    val fechaEvaluacion: Long? = null,
    val responsable: String = "",
    val revision: String = "1",
    val loading: Boolean = true
) {
    val ciudadesDisponibles: List<String> get() = MexicoLocations.ciudadesDe(entidadFederativa)
}

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
                clienteNombre = draft.clienteNombre,
                telefono = draft.telefono,
                ubicacion = draft.ubicacion,
                entidadFederativa = draft.entidadFederativa,
                ciudad = draft.ciudad,
                tipoInmueble = draft.tipoInmueble,
                fechaEvaluacion = draft.fechaEvaluacion ?: System.currentTimeMillis(),
                responsable = draft.responsable,
                revision = draft.revision,
                loading = false
            )
        }
    }

    fun onProjectNameChange(value: String) { _uiState.value = _uiState.value.copy(projectName = value) }
    fun onClienteNombreChange(value: String) { _uiState.value = _uiState.value.copy(clienteNombre = value) }
    fun onTelefonoChange(value: String) { _uiState.value = _uiState.value.copy(telefono = value) }
    fun onUbicacionChange(value: String) { _uiState.value = _uiState.value.copy(ubicacion = value) }

    /** Cambiar el estado reinicia la ciudad si ya no pertenece a las ciudades del nuevo estado. */
    fun onEntidadFederativaChange(value: String) {
        val state = _uiState.value
        val ciudadSigueValida = MexicoLocations.ciudadesDe(value).contains(state.ciudad)
        _uiState.value = state.copy(entidadFederativa = value, ciudad = if (ciudadSigueValida) state.ciudad else "")
    }

    fun onCiudadChange(value: String) { _uiState.value = _uiState.value.copy(ciudad = value) }
    fun onTipoInmuebleChange(value: String) { _uiState.value = _uiState.value.copy(tipoInmueble = value) }
    fun onFechaEvaluacionChange(value: Long) { _uiState.value = _uiState.value.copy(fechaEvaluacion = value) }
    fun onResponsableChange(value: String) { _uiState.value = _uiState.value.copy(responsable = value) }
    fun onRevisionChange(value: String) { _uiState.value = _uiState.value.copy(revision = value) }

    fun continueToQuestionnaire(onReady: (Long) -> Unit) {
        val state = _uiState.value
        val id = state.diagnosticId ?: return
        viewModelScope.launch {
            diagnosticRepository.updateProjectInfo(
                diagnosticId = id,
                projectName = state.projectName,
                ubicacion = state.ubicacion,
                responsable = state.responsable,
                revision = state.revision,
                clienteNombre = state.clienteNombre,
                telefono = state.telefono,
                entidadFederativa = state.entidadFederativa,
                ciudad = state.ciudad,
                tipoInmueble = state.tipoInmueble,
                fechaEvaluacion = state.fechaEvaluacion
            )
            onReady(id)
        }
    }
}
