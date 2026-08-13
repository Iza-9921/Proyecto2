package com.example.todoaccesible.ui.cliente.diagnostic.new

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoaccesible.data.local.seed.MexicoLocations
import com.example.todoaccesible.data.remote.ApiError
import com.example.todoaccesible.data.remote.ApiErrorMapper
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.data.repository.TipoCuestionarioRepository
import com.example.todoaccesible.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
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
    val logoEmpresaUri: String? = null,
    val loading: Boolean = true,
    /** El borrador ya traía datos capturados: se le pregunta al cliente si quiere continuarlo o empezar de nuevo. */
    val showResumeDialog: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null
) {
    val ciudadesDisponibles: List<String> get() = MexicoLocations.ciudadesDe(entidadFederativa)
}

class ProjectInfoViewModel(
    private val diagnosticRepository: DiagnosticRepository,
    private val userRepository: UserRepository,
    private val tipoCuestionarioRepository: TipoCuestionarioRepository,
    private val clienteId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectInfoUiState())
    val uiState: StateFlow<ProjectInfoUiState> = _uiState

    init {
        viewModelScope.launch {
            val draft = diagnosticRepository.getOrCreateDraft(clienteId)
            val tipoAsignado = resolveTipoAsignado()
            val hasProgress = diagnosticRepository.draftHasProgress(draft.id)
            _uiState.value = ProjectInfoUiState(
                diagnosticId = draft.id,
                projectName = draft.projectName,
                clienteNombre = draft.clienteNombre,
                telefono = draft.telefono,
                ubicacion = draft.ubicacion,
                entidadFederativa = draft.entidadFederativa,
                ciudad = draft.ciudad,
                // El tipo lo asigna el admin al activar la cuenta; no lo elige el cliente aquí.
                tipoInmueble = tipoAsignado,
                fechaEvaluacion = draft.fechaEvaluacion ?: System.currentTimeMillis(),
                responsable = draft.responsable,
                revision = draft.revision,
                logoEmpresaUri = draft.logoEmpresaUri,
                loading = false,
                showResumeDialog = hasProgress
            )
        }
    }

    /** Cuestionario asignado por el admmin; si no hay uno explícito, usa el primer tipo disponible (mismo fallback que `usersStore.getCuestionarioAsignado` en la web). */
    private suspend fun resolveTipoAsignado(): String {
        val asignado = userRepository.observeById(clienteId).firstOrNull()?.cuestionarioAsignado
        if (!asignado.isNullOrBlank()) return asignado
        return tipoCuestionarioRepository.getTipos().firstOrNull().orEmpty()
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
    fun onFechaEvaluacionChange(value: Long) { _uiState.value = _uiState.value.copy(fechaEvaluacion = value) }
    fun onResponsableChange(value: String) { _uiState.value = _uiState.value.copy(responsable = value) }
    fun onRevisionChange(value: String) { _uiState.value = _uiState.value.copy(revision = value) }
    fun onLogoEmpresaChange(value: String?) { _uiState.value = _uiState.value.copy(logoEmpresaUri = value) }

    fun dismissResumeDialog() { _uiState.value = _uiState.value.copy(showResumeDialog = false) }

    /** "Empezar de nuevo": descarta el borrador con avance y arranca uno vacío. */
    fun startOver() {
        val id = _uiState.value.diagnosticId ?: return
        viewModelScope.launch {
            diagnosticRepository.discardDraft(id)
            val fresh = diagnosticRepository.getOrCreateDraft(clienteId)
            _uiState.value = ProjectInfoUiState(
                diagnosticId = fresh.id,
                tipoInmueble = resolveTipoAsignado(),
                fechaEvaluacion = System.currentTimeMillis(),
                loading = false,
                showResumeDialog = false
            )
        }
    }

    fun continueToQuestionnaire(onReady: (Long) -> Unit) {
        val state = _uiState.value
        val id = state.diagnosticId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(submitting = true, error = null)
            try {
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
                    fechaEvaluacion = state.fechaEvaluacion,
                    logoEmpresaUri = state.logoEmpresaUri
                )
                _uiState.value = _uiState.value.copy(submitting = false)
                onReady(id)
            } catch (e: Exception) {
                val mapped = ApiErrorMapper.from(e)
                val message = if (mapped is ApiError.LicenciaVencida) {
                    "Tu licencia está vencida o inactiva: contacta al administrador para poder iniciar un diagnóstico."
                } else {
                    mapped.message
                }
                _uiState.value = _uiState.value.copy(submitting = false, error = message)
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
